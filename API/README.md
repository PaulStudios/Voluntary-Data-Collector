
---

# Voluntary Data Tracker API

This API is designed for managing projects and user data for the Voluntary Data Tracker application. The API allows you to create projects, upload user data, and retrieve project details.

## Table of Contents

- [Endpoints](#endpoints)
  - [Index Page](#index-page)
  - [Create a Project](#create-a-project)
  - [Get Project Details](#get-project-details)
  - [Upload User Data](#upload-user-data)
- [Getting Started](#getting-started)
- [Dependencies](#dependencies)

## Endpoints

### Index Page

**URL**: `/`

**Method**: `GET`

**Description**: Displays an index page listing all projects and provides a form to create a new project.

**Response**:
- **HTML**: Renders an HTML page listing all projects and a form for creating a new project.

### Create a Project

**URL**: `/project/`

**Method**: `POST`

**Description**: Creates a new project with the specified name and description.

**Request Parameters**:
- **Form Data**:
  - `project_name` (str): Name of the project (required, minimum length: 3).
  - `project_description` (str): Description of the project (required, minimum length: 10).

**Response**:
- **JSON**:
  - `message` (str): Success message.
  - `project_id` (str): The unique 6-digit ID of the created project.

### Get Project Details

**URL**: `/project/{project_id}`

**Method**: `GET`

**Description**: Retrieves the details of a specific project by its ID.

**Request Parameters**:
- **Path**:
  - `project_id` (str): The unique 6-digit ID of the project.

**Response**:
- **JSON**:
  - `project_id` (str): The unique ID of the project.
  - `project_name` (str): The name of the project.
  - `project_description` (str): The description of the project.

### Upload User Data

**URL**: `/project/{project_id}/user_data`

**Method**: `POST`

**Description**: Uploads user data to a specific project. Creates a table for the user if it doesn't exist.

**Request Parameters**:
- **Path**:
  - `project_id` (str): The unique 6-digit ID of the project.
- **Query**:
  - `user_id` (str): The unique ID of the user.
- **Body**:
  - `user_data` (UserData): A JSON object containing user data entries.

**Response**:
- **JSON**:
  - `message` (str): Success message.

### UserDataEntry Model

```python
class UserDataEntry(BaseModel):
    longitude: float
    latitude: float
    timestamp: str
```

### UserData Model

```python
class UserData(BaseModel):
    entries: List[UserDataEntry]
```

## Getting Started

To get started with the Voluntary Data Tracker API, follow these steps:

1. **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/voluntary-data-tracker.git
    cd voluntary-data-tracker
    ```

2. **Install dependencies**:
    ```bash
    pip install -r requirements.txt
    ```

3. **Run the server**:
    ```bash
    uvicorn main:app --reload
    ```

4. **Access the API documentation**:
    - Swagger UI: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
    - ReDoc: [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)

## Dependencies

- `fastapi`
- `sqlite3`
- `pydantic`
- `uvicorn`

---

