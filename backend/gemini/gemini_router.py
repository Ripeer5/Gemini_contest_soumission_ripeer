from typing import List
from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field
from fastapi.responses import StreamingResponse
from typing import Optional
import logging

from gemini.gemini_service import GeminiService

# Configuration du logger pour le suivi des opérations et des erreurs
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

gemini_router = APIRouter()

class RagRequest(BaseModel):
    query: str
    collection_name: Optional[str] = None
    prompt_template: Optional[str] = None 
    stream: Optional[bool] = True

class QueryRequest(BaseModel):
    query: str
    context: str = "Pas de contexte pour cette requête, répond simplement à la question"
    stream: bool = False

class ChatMessageRequest(BaseModel):
    message: str
    stream: bool = False

def get_gemini_service(request: Request) -> GeminiService:
    return request.state.injector.gemini_service

@gemini_router.post("/generate_rag")
async def generate_rag(request: RagRequest, service: GeminiService = Depends(get_gemini_service)):
    try:
        print(request)
        context=None
        if request.collection_name:
            context = await service.get_context(query=request.query, collection_name=request.collection_name)
            print(f"CONTEXTE = {context}")
        response_rag = service.get_response_stream(query=request.query, prompt_template=request.prompt_template, context=context, stream=request.stream)
        if request.stream:
            return StreamingResponse(response_rag, media_type="text/plain")
        else:
            return {"response": response_rag}
    except Exception as e:
        logger.error(f"Erreur lors de la génération de RAG : {e}")
        print(service.get_collection_names())
        print(f"Erreur lors de la génération de RAG : {e}")
        raise HTTPException(status_code=500, detail=f"Erreur interne du serveur lors de la génération de RAG. : {e}")

@gemini_router.post("/generate_stream")
async def generate_response(request: QueryRequest, service: GeminiService = Depends(get_gemini_service)):
    try:
        response_stream = service.get_response_stream(query=request.query, context=request.context, stream=request.stream)
        print(type(response_stream))
        print(response_stream)
        return StreamingResponse(response_stream, media_type="text/plain")
    except Exception as e:
        logger.error(f"Erreur lors de la génération de flux de réponse : {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur lors de la génération de flux de réponse.")

@gemini_router.post("/generate")
async def generate_response(request: QueryRequest, service: GeminiService = Depends(get_gemini_service)):
    print(request)
    try:
        response = service.get_response(query=request.query, context=request.context, stream=request.stream)
        print(response)
        return {"response": response}
    except Exception as e:
        logger.error(f"Erreur lors de la génération de réponse : {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur lors de la génération de réponse.")

@gemini_router.post("/start_chat")
async def start_chat(service: GeminiService = Depends(get_gemini_service)):
    try:
        chat_session = service.start_chat()
        return {"message": "Chat session started"}
    except Exception as e:
        logger.error(f"Erreur lors du démarrage de la session de chat : {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur lors du démarrage de la session de chat.")

@gemini_router.post("/send_message")
async def send_chat_message(request: ChatMessageRequest, service: GeminiService = Depends(get_gemini_service)):
    try:
        response = service.send_chat_message(request.message, stream=request.stream)
        return {"response": response}
    except ValueError as ve:
        logger.error(f"Erreur de validation : {ve}")
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as e:
        logger.error(f"Erreur lors de l'envoi du message de chat : {e}")
        raise HTTPException(status_code=500, detail="Erreur interne du serveur lors de l'envoi du message de chat.")
