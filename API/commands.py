import sqlite3
from database import get_db

def init_db():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute('''CREATE TABLE IF NOT EXISTS projects (
                        project_id TEXT PRIMARY KEY,
                        project_name TEXT,
                        project_description TEXT)''')
    conn.commit()
    conn.close()

def create_project_db(project_id: str):
    db_name = f'project_{project_id}.db'
    conn = sqlite3.connect(db_name)
    cursor = conn.cursor()
    cursor.execute('''CREATE TABLE IF NOT EXISTS user_data (
                        user_id TEXT PRIMARY KEY,
                        data_id TEXT,
                        longitude REAL,
                        latitude REAL,
                        timestamp TEXT)''')
    conn.commit()
    conn.close()

init_db()
