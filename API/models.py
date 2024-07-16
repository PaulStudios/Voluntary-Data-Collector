from sqlalchemy import Column, Integer, String, ForeignKey, Float, DateTime
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
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
    data_id = Column(String, index=True, unique=True)
    longitude = Column(Float, nullable=False)
    latitude = Column(Float, nullable=False)
    timestamp = Column(DateTime, nullable=False)
    upload_id = Column(String, nullable=False)
    user = relationship("User", back_populates="data")
    project = relationship("Project", back_populates="data")