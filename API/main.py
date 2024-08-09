import logging
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, PlainTextResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from database import database, Base, engine
from routers import project

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Voluntary Data Tracker API",
    description="API for managing projects and user data for the Voluntary Data Tracker application.",
    version="0.6.3",
    contact={
        "name": "PaulStudios [HilFing]",
        "url": "https://github.com/PaulStudios",
        "email": "paulstudiosofficial@gmail.com",
    },
)

Base.metadata.create_all(bind=engine)

app.include_router(project.router)

app.mount("/static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="templates")

@app.on_event("startup")
async def startup():
    await database.connect()

@app.on_event("shutdown")
async def shutdown():
    await database.disconnect()

@app.middleware("http")
async def log_requests(request, call_next):
    logger.info(f"Request: {request.method} {request.url}")
    response = await call_next(request)
    logger.info(f"Response status: {response.status_code}")
    return response

@app.get("/", response_class=HTMLResponse)
async def read_homepage(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})

@app.get("/status", response_class=PlainTextResponse)
async def healthcheck(request: Request):
    return "OK"

