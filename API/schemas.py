from pydantic import BaseModel, Field
from typing import List, Optional


class UserBase(BaseModel):
    username: str
    email: str
    full_name: Optional[str] = None
    disabled: Optional[bool] = None
    is_admin: Optional[bool] = None


class UserCreate(UserBase):
    password: str


class User(UserBase):
    id: int

    class Config:
        orm_mode = True


class Token(BaseModel):
    access_token: str
    token_type: str
    refresh_token: Optional[str] = None


class TokenData(BaseModel):
    username: Optional[str] = None


class UserDataEntry(BaseModel):
    longitude: float = Field(..., description="Longitude of the location")
    latitude: float = Field(..., description="Latitude of the location")
    timestamp: str = Field(..., description="Timestamp of the data entry")


class UserData(BaseModel):
    entries: List[UserDataEntry] = Field(..., description="List of user data entries")


class ProjectBase(BaseModel):
    project_name: str = Field(..., min_length=3, description="Name of the project")
    project_description: str = Field(..., min_length=10, description="Description of the project")


class Project(ProjectBase):
    id: int

    class Config:
        orm_mode = True
