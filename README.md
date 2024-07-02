# Voluntary Data Tracker App

This project is a Voluntary Data Tracker App that allows users to enter a project ID to load the applicable data tracker. The backend is built using FastAPI, and the data is stored in an SQLite database. The Android app uses Retrofit to communicate with the backend and display the relevant data tracker.

## Features

- User can enter a project ID to load the associated data tracker.
- Backend API built with FastAPI for handling project data.
- SQLite database to store project details and tracker data.
- Android app with user-friendly interface to interact with the backend.
- Retrofit integration for seamless API communication.

## Prerequisites

- Python 3.8 or higher
- FastAPI
- Uvicorn
- SQLite
- Android Studio
- Kotlin

## Getting Started

### Backend Setup

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/voluntary-data-tracker-app.git
    cd voluntary-data-tracker-app
    ```

2. Create and activate a virtual environment:
    ```sh
    python -m venv venv
    source venv/bin/activate  # On Windows use `venv\Scripts\activate`
    ```

3. Install dependencies:
    ```sh
    pip install fastapi uvicorn sqlite3
    ```

4. Initialize the database:
    ```python
    from main import init_db
    init_db()
    ```

5. Start the FastAPI server:
    ```sh
    uvicorn main:app --reload
    ```

### Android App Setup

1. Open Android Studio and clone the repository.
2. Update the `ApiService` interface with your server IP address.
3. Implement the logic to fetch and display the tracker data based on the project ID input by the user.

## API Endpoints

- **Get Project by ID**
  ```http
  GET /project/{project_id}
