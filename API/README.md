# Voluntary Data Tracker API

## Overview

The Voluntary Data Tracker API is a FastAPI-based application designed for managing projects and user data. This API provides endpoints for user authentication, project management, and data uploads. It uses SQLAlchemy for database operations and Jinja2 for templating.

## Features

- **User Authentication**: Secure user login with JWT-based access and refresh tokens.
- **Project Management**: Create, retrieve, and manage projects.
- **User Data Uploads**: Upload and retrieve user data for specific projects.
- **Admin Functions**: Admin routes for user creation and deletion.
- **Logging**: Comprehensive logging for monitoring requests and actions.
- **Templating**: HTML responses using Jinja2 templates.
- **Static Files**: Support for serving static files.

## Installation

1. **Clone the repository**:

    ```bash
    git clone https://github.com/PaulStudios/voluntary-data-tracker-api.git
    cd voluntary-data-tracker-api
    ```

2. **Create and activate a virtual environment**:

    ```bash
    python -m venv venv
    source venv/bin/activate  # On Windows: venv\Scripts\activate
    ```

3. **Install the dependencies**:

    ```bash
    pip install -r requirements.txt
    ```

4. **Run the application**:

    ```bash
    uvicorn main:app --reload
    ```

5. **Access the application**:
    Open your web browser and go to [http://localhost:8000](http://localhost:8000)

## Configuration

The application uses environment variables for configuration. You can create a `.env` file in the project root with the following contents:

```env
DATABASE_URL=sqlite:///./test.db
SECRET_KEY=your_secret_key
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30
```

## Endpoints

### Authentication

- **Login**: `POST /token`
  - Authenticates a user and returns access and refresh tokens.

- **Refresh Token**: `POST /refresh`
  - Refreshes the access token using the refresh token.

### Admin

- **Create User**: `POST /admin/users/`
  - Creates a new user. Admin-only route.

- **Delete User**: `DELETE /admin/users/{username}`
  - Deletes a user by username. Admin-only route.

### Projects

- **List Projects**: `GET /project/`
  - Retrieves and renders a list of all projects.

- **Create Project**: `POST /project/`
  - Creates a new project.

- **Retrieve Project Data**: `GET /project/{project_id}`
  - Retrieves data for a specific project.

- **Upload User Data**: `POST /project/{project_id}/user_data`
  - Uploads user data to a specific project.

- **Retrieve User Data**: `GET /project/{project_id}/user_data/{user_id}`
  - Retrieves user data for a specific project and user.

## Logging

Logging is configured at the `INFO` level. Logs are output to the console and provide information about requests and significant actions within the application.

## Middleware

A middleware is included for logging each HTTP request and response status.

## Templating

The application uses Jinja2 templates to render HTML responses. Templates are located in the `templates` directory.

## Static Files

Static files are served from the `static` directory.

## Contributing

Contributions are welcome! Please create a pull request or open an issue to discuss changes.

## License

This project is licensed under the GNU GPLv3 License. See the `LICENSE` file for details.

## Contact

- **PaulStudios [HilFing]**
  - **GitHub**: [PaulStudios](https://github.com/PaulStudios)
  - **Email**: paulstudiosofficial@gmail.com

---

Feel free to reach out for any queries or support!
