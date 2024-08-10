from fastapi import FastAPI, APIRouter, HTTPException
from pydantic import BaseModel

health_router = APIRouter()

class HealthResponse(BaseModel):
    status: str

@health_router.get("/", response_model=HealthResponse)
async def health_check():
    try:
        # Vous pouvez ajouter ici des vérifications spécifiques, par exemple :
        # - Vérification de la connexion à la base de données
        # - Vérification de la disponibilité de services externes
        # Pour l'instant, nous renvoyons simplement un statut "healthy"
        return HealthResponse(status="healthy")
    except Exception as e:
        raise HTTPException(status_code=500, detail="Service unavailable")

