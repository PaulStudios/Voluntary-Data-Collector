# Voluntary Data Tracker API

## Overview

The Voluntary Data Tracker API is a FastAPI-based application for managing projects and user data. This API includes endpoints for user authentication, project management, and data uploads. It uses SQLAlchemy for database operations and Jinja2 for templating.

## Features

- User Authentication (Login, Token Refresh)
- Admin Operations (Create User, Delete User)
- Project Management (Create Project, Upload User Data, Retrieve User Data)
- Static Files and Template Rendering

## Requirements

- Python 3.8+
- FastAPI
- SQLAlchemy
- Jinja2
- Uvicorn (for running the app)

## Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/PaulStudios/voluntary-data-tracker.git
    cd voluntary-data-tracker
    ```

2. Create and activate a virtual environment:

    ```bash
    python -m venv venv
    source venv/bin/activate  # On Windows: venv\Scripts\activate
    ```

3. Install the dependencies:

    ```bash
    pip install -r requirements.txt
    ```

## Running the Application

Start the FastAPI server using Uvicorn:

```bash
uvicorn main:app --reload
````

The application will be available at `http://127.0.0.1:8000`.

## API Documentation

FastAPI generates interactive API documentation:

- Swagger UI: `http://127.0.0.1:8000/docs`
- ReDoc: `http://127.0.0.1:8000/redoc`

## Endpoints

### Homepage

- **GET /**: Returns the homepage with links to all pages.

    ```http
    GET /
    ```

### Users Homepage

- **GET /users**: Returns the users' homepage.

    ```http
    GET /users
    ```

### Projects Homepage

- **GET /projects**: Returns the projects' homepage.

    ```http
    GET /projects
    ```

### Admin Homepage

- **GET /admin**: Returns the admin homepage.

    ```http
    GET /admin
    ```

### Admin Operations

#### Create User

- **POST /admin/users/**: Create a new user.

    ```http
    POST /admin/users/
    ```

    Request Body (form-data):

    - `username`: str
    - `password`: str
    - `email`: str
    - `full_name`: str
    - `is_admin`: bool (optional)

    Response:

    ```json
    {
        "username": "new_user",
        "email": "new_user@example.com",
        "full_name": "New User",
        "is_admin": false
    }
    ```

#### Delete User

- **DELETE /admin/users/{username}**: Delete a user by username.

    ```http
    DELETE /admin/users/{username}
    ```

    Response:

    ```json
    {
        "message": "User {username} deleted successfully"
    }
    ```

### Project Operations

#### Create Project

- **POST /project/**: Create a new project.

    ```http
    POST /project/
    ```

    Request Body (form-data):

    - `project_name`: str
    - `project_description`: str

    Response:

    ```json
    {
        "project_name": "New Project",
        "project_description": "Description of the new project"
    }
    ```

#### Retrieve User Data for a Project

- **GET /project/{project_id}/user_data/{user_id}**: Retrieve user data for a specific project and user. Optionally filter by upload ID.

    ```http
    GET /project/{project_id}/user_data/{user_id}
    ```

    Query Parameters:

    - `upload_id` (optional): str

    Response:

    ```json
    {
        "data": [
            {
                "data_id": "uuid",
                "longitude": "longitude",
                "latitude": "latitude",
                "timestamp": "timestamp",
                "upload_id": "upload_id"
            }
        ]
    }
    ```

#### Upload User Data to a Project

- **POST /project/{project_id}/user_data**: Upload user data to a specific project.

    ```http
    POST /project/{project_id}/user_data
    ```

    Request Body (form-data):

    - `user_id`: int
    - `upload_id`: str
    - `user_data`: JSON (Refer to schema `UserData`)

    Response:

    ```json
    {
        "message": "User data uploaded successfully"
    }
    ```

### User Operations

#### Login for Access Token

- **POST /token**: Authenticate user and return access token and refresh token.

    ```http
    POST /token
    ```

    Request Body (form-data):

    - `username`: str
    - `password`: str

    Response:

    ```json
    {
        "access_token": "access_token",
        "token_type": "bearer",
        "refresh_token": "refresh_token"
    }
    ```

#### Refresh Access Token

- **POST /refresh**: Refresh access token using refresh token.

    ```http
    POST /refresh
    ```

    Request Body (form-data):

    - `refresh_token`: str

    Response:

    ```json
    {
        "access_token": "access_token",
        "token_type": "bearer"
    }
    ```

## Contact

For any inquiries or issues, please contact:

- Email: [paulstudiosofficial@gmail.com](mailto:paulstudiosofficial@gmail.com)
- GitHub: [PaulStudios](https://github.com/PaulStudios)

