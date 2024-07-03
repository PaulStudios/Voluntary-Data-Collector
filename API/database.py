import sqlite3
import os
from fastapi import HTTPException


def get_db():
    return sqlite3.connect('voluntary_data_tracker.db', check_same_thread=False)


def get_project_db(project_id: str):
    db_name = f'project_{project_id}.db'
    if not os.path.exists(db_name):
        raise HTTPException(status_code=404, detail="Project database not found")
    return sqlite3.connect(db_name, check_same_thread=False)
