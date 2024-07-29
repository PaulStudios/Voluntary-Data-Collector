import json
import logging
import random
import uuid
from asyncpg import ForeignKeyViolationError
from database import database, get_db
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, Form, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from models import Project, User
from schemas import ProjectBase, UserData as UserDataSchema
from sqlalchemy.orm import Session

router = APIRouter(prefix="/project", tags=["Project"])
templates = Jinja2Templates(directory="templates")
logger = logging.getLogger(__name__)


def generate_unique_project_id(db: Session) -> int:
    while True:
        project_id = random.randint(100000, 999999)
        if not db.query(Project).filter(Project.id == project_id).first():
            return project_id


@router.get("/", response_class=HTMLResponse, summary="List all projects and form to create new project.")
async def read_projects(request: Request, db: Session = Depends(get_db)):
    projects = db.query(Project).all()
    return templates.TemplateResponse("project_index.html", {"request": request, "projects": projects})


@router.post("/", response_model=ProjectBase)
async def create_project(
        project_name: str = Form(...),
        project_description: str = Form(...),
        db: Session = Depends(get_db)
):
    project_id_new = generate_unique_project_id(db)
    query = """
    INSERT INTO projects (id, project_name, project_description) 
    VALUES (:project_id_new, :project_name, :project_description) 
    RETURNING id
    """
    values = {"project_id_new": project_id_new, "project_name": project_name,
              "project_description": project_description}
    project_id = await database.execute(query=query, values=values)
    logger.info(f"Project created: {project_name} with ID {project_id}")
    return {"id": project_id, "project_name": project_name, "project_description": project_description}


@router.get("/{project_id}/get_data/{user_id}/{upload_id}",
            summary="Retrieve user data for a project for a specific upload id")
async def get_upload_data(project_id: int, user_id: int, upload_id: str):
    query = "SELECT * FROM user_data WHERE project_id = :project_id AND user_id = :user_id"
    values = {"project_id": project_id, "user_id": user_id}
    query += " AND upload_id = :upload_id"
    values["upload_id"] = upload_id
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="No data found")
    logger.info(f"Data retrieved for project {project_id}, user {user_id}, upload_id {upload_id}")
    return data


@router.get("/{project_id}/get_data/{user_id}", summary="Retrieve user data list for a project")
async def get_user_data(project_id: int, user_id: int):
    query = "SELECT * FROM user_data WHERE project_id = :project_id AND user_id = :user_id"
    values = {"project_id": project_id, "user_id": user_id}
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="No data found")
    logger.info(f"Data retrieved for project {project_id}, user {user_id}")
    records = data
    # Dictionary to store the latest timestamp for each upload
    latest_timestamps = {}
    # Iterate through each record
    for record in records:
        user_id = record['upload_id']
        timestamp = record['timestamp']
        # Update the latest timestamp for the user
        if user_id not in latest_timestamps or timestamp > latest_timestamps[user_id]:
            latest_timestamps[user_id] = timestamp
    # unique upload ids with their latest timestamps
    result = [{"upload_id": user_id, "latest_timestamp": latest_timestamps[user_id].strftime('%Y-%m-%d %H:%M:%S')} for
              user_id in latest_timestamps]
    return result


@router.get("/{project_id}/get_data", summary="Retrieve list of all data for a project")
async def list_user_data(project_id: int):
    query = "SELECT * FROM user_data WHERE project_id = :project_id"
    values = {"project_id": project_id}
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="Project not found")
    # Parse JSON data
    records = data
    # Dictionary to store the latest timestamp for each user
    latest_timestamps = {}
    # Iterate through each record
    for record in records:
        user_id = record['user_id']
        timestamp = record['timestamp']
        # Update the latest timestamp for the user
        if user_id not in latest_timestamps or timestamp > latest_timestamps[user_id]:
            latest_timestamps[user_id] = timestamp
    # unique user ids with their latest timestamps
    result = [{"user_id": user_id, "latest_timestamp": latest_timestamps[user_id].strftime('%Y-%m-%d %H:%M:%S')} for
              user_id in latest_timestamps]
    logger.info(f"Data list retrieved for project {project_id}")
    return result


@router.post("/{project_id}/user_data", summary="Upload user data to a project")
async def upload_user_data(
        project_id: int,
        user_id: int = Form(..., description="ID of the user"),
        upload_id: str = Form(..., description="ID of the upload batch"),
        user_data: str = Form(..., description="User data entries to be uploaded as a JSON string"),
        db: Session = Depends(get_db)
):
    try:
        user_data_dict = json.loads(user_data)
        validated_user_data = UserDataSchema(**user_data_dict)

        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            user = User(id=user_id)
            db.add(user)
            db.commit()
            db.refresh(user)
            logger.info(f"Created new user with ID {user_id}")

        query = "INSERT INTO user_data (user_id, project_id, data_id, longitude, latitude, timestamp, upload_id) VALUES (:user_id, :project_id, :data_id, :longitude, :latitude, :timestamp, :upload_id)"
        for entry in validated_user_data.entries:
            data_id = str(uuid.uuid4())
            values = {
                "user_id": user_id,
                "project_id": project_id,
                "data_id": data_id,
                "longitude": entry.longitude,
                "latitude": entry.latitude,
                "timestamp": datetime.fromisoformat(entry.timestamp),
                "upload_id": upload_id
            }
            await database.execute(query=query, values=values)

        logger.info(f"User data uploaded for project {project_id}, user {user_id}, upload_id {upload_id}")
        return {
            "message": f"User data uploaded successfully for project {project_id}, user {user_id}, upload_id {upload_id}"}
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON in user_data")
    except ForeignKeyViolationError:
        raise HTTPException(status_code=400, detail="Project does not exist")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/{project_id}", summary="Retrieve project data for a project")
async def get_project_data(project_id: int):
    query = "SELECT * FROM projects WHERE id = :project_id"
    values = {"project_id": project_id}
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="Invalid project ID")
    logger.info(f"Data retrieved for project {project_id}")
    return data[0]


@router.delete("/{project_id}/delete_data", summary="Delete all data for a project")
async def delete_project_data(project_id: int):
    return HTTPException(status_code=403, detail="This feature has been disabled")
    query = "DELETE FROM user_data WHERE project_id = :project_id"
    values = {"project_id": project_id}
    try:
        await database.execute(query=query, values=values)
        logger.info(f"All data deleted for project {project_id}")
        return {"message": f"All data deleted successfully for project {project_id}"}
    except Exception as e:
        logger.error(f"Error deleting data for project {project_id}: {str(e)}")
        raise HTTPException(status_code=500, detail="Error deleting project data")