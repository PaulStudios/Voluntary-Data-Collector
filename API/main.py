from fastapi import FastAPI, HTTPException, Form, Depends, Request
from fastapi.responses import HTMLResponse
import random
import sqlite3
import uuid
from pydantic import BaseModel
from typing import List
from database import get_db, get_project_db
from commands import create_project_db

app = FastAPI()

class UserDataEntry(BaseModel):
    longitude: float
    latitude: float
    timestamp: str

class UserData(BaseModel):
    entries: List

class Project(BaseModel):
    project_name: str
    project_description: str

@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT project_id, project_name, project_description FROM projects")
    projects = cursor.fetchall()
    conn.close()
    project_list = "".join(
        f"<li><strong>{project[1]}</strong>: {project[2]} (ID: {project[0]})</li>" for project in projects
    )
    html_content = f"""
    <html>
        <head>
            <title>Project Index</title>
            <style>
                body {{
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f4f4f9;
                }}
                h1 {{
                    color: #333;
                }}
                ul {{
                    list-style-type: none;
                    padding: 0;
                }}
                li {{
                    background: #fff;
                    margin: 10px 0;
                    padding: 10px;
                    border-radius: 5px;
                    box-shadow: 0 0 5px rgba(0,0,0,0.1);
                }}
                form {{
                    background: #fff;
                    padding: 20px;
                    border-radius: 5px;
                    box-shadow: 0 0 5px rgba(0,0,0,0.1);
                }}
                label {{
                    display: block;
                    margin: 10px 0 5px;
                }}
                input[type="text"], input[type="submit"] {{
                    width: 100%;
                    padding: 10px;
                    margin: 5px 0 10px;
                    border: 1px solid #ccc;
                    border-radius: 5px;
                }}
                input[type="submit"] {{
                    background-color: #28a745;
                    color: white;
                    border: none;
                    cursor: pointer;
                }}
                input[type="submit"]:hover {{
                    background-color: #218838;
                }}
            </style>
        </head>
        <body>
            <h1>Project Index</h1>
            <ul>
                {project_list}
            </ul>
            <h2>Create a new project</h2>
            <form action="/project/" method="post">
                <label for="project_name">Project Name:</label>
                <input type="text" id="project_name" name="project_name" required minlength="3"><br>
                <label for="project_description">Project Description:</label>
                <input type="text" id="project_description" name="project_description" required minlength="10"><br>
                <input type="submit" value="Create Project">
            </form>
        </body>
    </html>
    """
    return HTMLResponse(content=html_content)

@app.post("/project/")
async def create_project(project_name: str = Form(...), project_description: str = Form(...), db: sqlite3.Connection = Depends(get_db)):
    project_id = f"{random.randint(100000, 999999):06d}"
    cursor = db.cursor()
    cursor.execute("INSERT INTO projects (project_id, project_name, project_description) VALUES (?, ?, ?)",
                   (project_id, project_name, project_description))
    db.commit()
    db.close()
    # Create a separate database for the project
    create_project_db(project_id)
    return {"message": "Project created successfully", "project_id": project_id}

@app.get("/project/{project_id}")
async def get_project(project_id: str, db: sqlite3.Connection = Depends(get_db)):
    cursor = db.cursor()
    cursor.execute("SELECT * FROM projects WHERE project_id = ?", (project_id,))
    project = cursor.fetchone()
    db.close()
    if project is None:
        raise HTTPException(status_code=404, detail="Project not found")
    return {
        "project_id": project[0],
        "project_name": project[1],
        "project_description": project[2]
    }

@app.post("/project/{project_id}/user_data")
async def upload_user_data(project_id: str, user_id: str, user_data: UserData):
    conn = get_project_db(project_id)
    cursor = conn.cursor()
    # Create a table for the user if it doesn't exist
    cursor.execute(f'''CREATE TABLE IF NOT EXISTS user_{user_id} (
                        data_id TEXT PRIMARY KEY,
                        longitude REAL,
                        latitude REAL,
                        timestamp TEXT)''')
    for entry in user_data.entries:
        data_id = str(uuid.uuid4())
        cursor.execute(f"INSERT INTO user_{user_id} (data_id, longitude, latitude, timestamp) VALUES (?, ?, ?, ?)",
                       (data_id, entry.longitude, entry.latitude, entry.timestamp))
    conn.commit()
    conn.close()
    return {"message": "User data uploaded successfully"}

# Run the server
# uvicorn main:app --reload
