import logging
from fastapi import FastAPI, Request, Depends
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from database import database, Base, engine, get_db
from models import User
from auth import get_password_hash
from routers import user, admin, project

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Voluntary Data Tracker API",
    description="API for managing projects and user data for the Voluntary Data Tracker application.",
    version="0.6.3  ",
    contact={
        "name": "PaulStudios [HilFing]",
        "url": "https://github.com/PaulStudios",
        "email": "paulstudiosofficial@gmail.com",
    },
)

# Create the database tables
Base.metadata.create_all(bind=engine)

# Include routers
app.include_router(user.router)
app.include_router(admin.router)
app.include_router(project.router)

# Mount static files and templates
app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")


@app.on_event("startup")
async def startup():
    await database.connect()
    create_initial_admin_users()


@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()


def create_initial_admin_users():
    """Create initial admin users."""
    db = next(get_db())
    if not db.query(User).filter(User.username == "app_admin").first():
        app_admin = User(
            username="app_admin",
            email="app_admin@example.com",
            full_name="App Admin",
            hashed_password=get_password_hash("app_admin_password"),
            is_admin=True,
        )
        db.add(app_admin)
        db.commit()
        db.refresh(app_admin)
        logger.info("Created initial admin user: app_admin")

    if not db.query(User).filter(User.username == "personal_admin").first():
        personal_admin = User(
            username="personal_admin",
            email="personal_admin@example.com",
            full_name="Personal Admin",
            hashed_password=get_password_hash("personal_admin_password"),
            is_admin=True,
        )
        db.add(personal_admin)
        db.commit()
        db.refresh(personal_admin)
        logger.info("Created initial admin user: personal_admin")


# Middleware for logging requests
@app.middleware("http")
async def log_requests(request, call_next):
    logger.info(f"Request: {request.method} {request.url}")
    response = await call_next(request)
    logger.info(f"Response status: {response.status_code}")
    return response


# Homepage route
@app.get("/", response_class=HTMLResponse)
async def read_homepage(request: Request):
    """Homepage with links to all pages"""
    return templates.TemplateResponse("index.html", {"request": request})


# Users homepage route
@app.get("/users", response_class=HTMLResponse, tags=["users"])
async def users_homepage(request: Request):
    """Users Homepage"""
    return templates.TemplateResponse("users_homepage.html", {"request": request})


# Projects homepage route
@app.get("/projects", response_class=HTMLResponse, tags=["projects"])
async def projects_homepage(request: Request):
    """Projects Homepage"""
    return templates.TemplateResponse("projects_homepage.html", {"request": request})


# Admin homepage route
@app.get("/admin", response_class=HTMLResponse, tags=["admin"])
async def admin_homepage(request: Request):
    """Admin Homepage"""
    return templates.TemplateResponse("admin_homepage.html", {"request": request})
