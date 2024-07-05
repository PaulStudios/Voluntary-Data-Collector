from sqlalchemy import Boolean, Column, ForeignKey, Integer, String
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, unique=True, index=True, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    full_name = Column(String, index=True)
    hashed_password = Column(String, nullable=False)
    refresh_token = Column(String, nullable=True)
    disabled = Column(Boolean, default=False)
    is_admin = Column(Boolean, default=False)

    data = relationship("UserData", back_populates="user")

class Project(Base):
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    project_name = Column(String, unique=True, index=True, nullable=False)
    project_description = Column(String, nullable=False)

    data = relationship("UserData", back_populates="project")

class UserData(Base):
    __tablename__ = "user_data"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    project_id = Column(Integer, ForeignKey("projects.id"), nullable=False)
    data_id = Column(String, index=True)
    longitude = Column(String, nullable=False)
    latitude = Column(String, nullable=False)
    timestamp = Column(String, nullable=False)
    upload_id = Column(String, nullable=False)

    user = relationship("User", back_populates="data")
    project = relationship("Project", back_populates="data")
