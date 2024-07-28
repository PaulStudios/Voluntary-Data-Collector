# **Voluntary Data Collector**

**Author:** Indradip Paul [HilFing] - PaulStudios  
**Release Date:** 22-07-24  

## **Purpose:**  
The Voluntary Data Collector is designed to efficiently manage and organize voluntary data collection projects. The collected data will primarily be used for training AI and machine learning models. Researchers, organizations, and developers can utilize this tool to gather data from users to enhance their AI systems.

## **Structure:**  
The project consists of two main components:
#### 1. **Android App:** Provides a clean, user-friendly interface with login/register options, including Google and GitHub logins. Users can review collected data before uploading and have the flexibility to start and stop data collection at their convenience.
#### 2. **FastAPI Server with Database:** Handles backend operations with PostgreSQL, supporting a large number of concurrent requests. The server is easily deployable with a production-ready Docker container.

## **Security and Privacy:**  
Data is transferred using HTTPS, ensuring secure communication. Anonymization of data is enforced strictly, and user passwords are encrypted using Firebase Authentication, prioritizing user privacy.

## **Deployment and Scalability:**  
The API server is scalable, easily deployable through Docker, and capable of handling multiple database operations efficiently. This ensures that the system remains robust and responsive under heavy usage.

## **Community and Support:**  
Issues and security vulnerabilities can be reported directly through GitHub. Feedback and constructive criticism are encouraged. Contributions are welcome, and developers can contact the author via email for support and collaboration.

## **Future Plans:**  
- Allow simultaneous data collection for multiple projects within the app.
- Increase data upload speed through better structuring.
- Streamline the permission-fetching process in the app.
- Add authentication in the API for data retrieval and project creation.
- Improve the upload progress bar to accurately reflect progress.
