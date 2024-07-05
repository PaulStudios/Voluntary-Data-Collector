from fastapi import APIRouter, Depends, HTTPException, Form
from sqlalchemy.orm import Session
from auth import get_current_admin_user, get_password_hash
from database import get_db
from models import User
from schemas import User as UserSchema, UserCreate
import logging

router = APIRouter(prefix="/admin", tags=["Admin"])

logger = logging.getLogger(__name__)

@router.post("/users/", response_model=UserSchema)
async def create_user(
    username: str = Form(...),
    password: str = Form(...),
    email: str = Form(...),
    full_name: str = Form(...),
    is_admin: bool = Form(False),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_admin_user)
):
    """Create a new user."""
    db_user = db.query(User).filter(User.username == username).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")
    hashed_password = get_password_hash(password)
    new_user = User(
        username=username,
        email=email,
        full_name=full_name,
        hashed_password=hashed_password,
        is_admin=is_admin
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    logger.info(f"Admin {current_user.username} created user {username}")
    return new_user

@router.delete("/users/{username}", summary="Delete a user by username")
async def delete_user(username: str, db: Session = Depends(get_db), current_user: User = Depends(get_current_admin_user)):
    """Delete a user by their username."""
    db_user = db.query(User).filter(User.username == username).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    db.delete(db_user)
    db.commit()
    logger.info(f"Admin {current_user.username} deleted user {username}")
    return {"message": f"User {username} deleted successfully"}
