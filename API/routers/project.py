import logging
import random
import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Form, Request
from fastapi import Body
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from fastapi.templating import Jinja2Templates
from auth import get_current_active_user
from database import database, get_db
from models import Project
from models import User
from schemas import ProjectBase
from schemas import UserData

router = APIRouter(prefix="/project", tags=["Project"])
templates = Jinja2Templates(directory="templates")
logger = logging.getLogger(__name__)


def generate_unique_project_id(db: Session) -> int:
    """Generate a unique 6-digit project ID."""
    while True:
        project_id = random.randint(100000, 999999)
        if not db.query(Project).filter(Project.id == project_id).first():
            return project_id


@router.get("/", response_class=HTMLResponse)
async def read_projects(request: Request, db: Session = Depends(get_db)):
    """Render the project index page."""
    projects = db.query(Project).all()
    return templates.TemplateResponse("project_index.html", {"request": request, "projects": projects})


@router.post("/", response_model=ProjectBase)
async def create_project(
        project_name: str = Form(...),
        project_description: str = Form(...),
        db: Session = Depends(get_db)
):
    """Create a new project."""
    project_id_new = generate_unique_project_id(db)
    print(project_id_new)
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


@router.get("/{project_id}/user_data/{user_id}", summary="Retrieve user data for a project")
async def get_user_data(project_id: int, user_id: int, upload_id: Optional[str] = None,
                        current_user: User = Depends(get_current_active_user)):
    """Retrieve user data for a specific project and user. Optionally filter by upload ID."""
    query = "SELECT * FROM user_data WHERE project_id = :project_id AND user_id = :user_id"
    values = {"project_id": project_id, "user_id": user_id}
    if upload_id:
        query += " AND upload_id = :upload_id"
        values["upload_id"] = upload_id
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="No data found")
    logger.info(f"Data retrieved for project {project_id}, user {user_id}, upload_id {upload_id}")
    return {"data": data}


@router.post("/{project_id}/user_data", summary="Upload user data to a project")
async def upload_user_data(
        project_id: int,
        user_id: int = Form(..., description="ID of the user"),
        upload_id: str = Form(..., description="ID of the upload batch"),
        user_data: UserData = Body(..., description="User data entries to be uploaded"),
        current_user: User = Depends(get_current_active_user)
):
    """Upload user data to a specific project."""
    query = "INSERT INTO user_data (user_id, project_id, data_id, longitude, latitude, timestamp, upload_id) VALUES (:user_id, :project_id, :data_id, :longitude, :latitude, :timestamp, :upload_id)"
    for entry in user_data.entries:
        data_id = str(uuid.uuid4())
        values = {
            "user_id": user_id,
            "project_id": project_id,
            "data_id": data_id,
            "longitude": entry.longitude,
            "latitude": entry.latitude,
            "timestamp": entry.timestamp,
            "upload_id": upload_id
        }
        await database.execute(query=query, values=values)
    logger.info(f"User data uploaded for project {project_id}, user {user_id}, upload_id {upload_id}")
    return {"message": "User data uploaded successfully"}


@router.get("/{project_id}", summary="Retrieve project data for a project")
async def get_project_data(project_id: int):
    query = "SELECT * FROM projects WHERE id = :project_id"
    values = {"project_id": project_id}
    data = await database.fetch_all(query=query, values=values)
    if not data:
        raise HTTPException(status_code=404, detail="Invalid project ID")
    logger.info(f"Data retrieved for project {project_id}")
    return data[0]
