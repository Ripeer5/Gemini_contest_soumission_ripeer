from injector import singleton, inject
import pathlib

import uuid
import google.generativeai as genai
from langchain.prompts import PromptTemplate
from prompt.prompt_library import prompt_template_SPEC20, default_prompt
from prompt.artworks_context import artwork_global_context
from vectorstore.qdrant_service import QdrantService
from langchain.schema.document import Document
from google.cloud import firestore


import os
@singleton
class GeminiService:
    @inject
    def __init__(self, qdrant_service: QdrantService, model_name='gemini-1.5-flash') -> None:
        self.model_name = model_name
        GOOGLE_API_KEY = os.getenv('GOOGLE_GEMINI_API_KEY')
        genai.configure(api_key=GOOGLE_API_KEY)
        self.model = genai.GenerativeModel('gemini-1.5-flash')
        self.qdrant_service = qdrant_service
        self.db = self.initialize_firestore()
        
    @staticmethod
    def initialize_firestore():
        db = firestore.Client()
        return db

    def get_artwork_by_id(self, artwork_id: str):
        doc_ref = self.db.collection('artworks').document(artwork_id)
        doc = doc_ref.get()
        if doc.exists:
            return Artwork.from_dict(doc.to_dict())
        else:
            raise ValueError(f"Artwork with ID {artwork_id} not found")
        
    def get_artwork_id_by_collection_name(self, collection_name: str):
        artworks_ref = self.db.collection('artworks')
        print(f"VOICI LES ARTWORKS : {artworks_ref.document()}")
        query = artworks_ref.where('collection_name', '==', collection_name).limit(1)
        results = query.get()
        if results:
            for doc in results:
                return doc.id
        raise ValueError(f"No artwork found with collection name {collection_name}")

    def get_global_artwork_context(self, artwork_id: str):
        try:
            artwork = self.get_artwork_by_id(artwork_id)
            artwork_description = artwork.description
            museum_context = artwork.museum.museum_context
            artist_biography = artwork.artist.artist_biography

            context_text = (
                f"Voici la description de l'oeuvre: {artwork_description}\n\n"
                f"Voici le contexte du musée: {museum_context}\n\n"
                f"Voici la biographie de l'artiste: {artist_biography}"
            )

            artwork_context_document = Document(page_content=context_text, metadata={})
            return artwork_context_document
        except ValueError as e:
            print(e)
            return Document(page_content=str(e), metadata={})
        
    async def get_context(self, query: str, collection_name: str):
        context = []
        artwork_id = self.get_artwork_id_by_collection_name(collection_name=collection_name)
        artwork_context = self.get_global_artwork_context(artwork_id)
        context_from_qdrant = await self.qdrant_service.get_relevant_documents_and_neighbor_from_collection(query=query, collection_name=collection_name)
        context.append(artwork_context)
        context.append(context_from_qdrant)
        return context
    
    async def get_rag_from_collection(self, collection_name: str, query: str, prompt_template: str = None, 
                                      context: str = default_prompt, stream: bool = False):
        context = await self.qdrant_service.get_relevant_documents_and_neighbor_from_collection(query=query, collection_name=collection_name)
        response = self.get_response(query, context=context, prompt_template=prompt_template, stream=stream)
        return response
        
    def get_response_stream(self, query: str, prompt_template: str = None, context: str = default_prompt, 
                            stream: bool = False):
        final_prompt = query
        if prompt_template:
            import prompt.prompt_library as prompt_library
            try:
                prompt_template = getattr(prompt_library, prompt_template)
                prompt = PromptTemplate(
                    input_variables=["context", "question"],
                    template=prompt_template,
                )
                                
                final_prompt = prompt.format(context=context, query=query)
            except AttributeError:
                print(f"Le prompt '{prompt_template}' n'existe pas dans prompt_library")
        
        response_generator = self.model.generate_content(final_prompt, stream=True)
        
        for response in response_generator:
            if response._result and response._result.candidates:
                for candidate in response._result.candidates:
                    if candidate.content.parts:
                        yield ''.join(part.text for part in candidate.content.parts)
            else:
                yield "Pas de réponse valide générée."
        
    def get_response(self, query: str, prompt_template: str = None, context: str = "Pas de contexte pour cette requête, répond simplement à la question",
                     stream: bool = False, image_path: str = None):

        contents = [query]
        
        if image_path:
            image_data = {
                'mime_type': 'image/png',
                'data': pathlib.Path(image_path).read_bytes()
            }
            contents.append(image_data)

        if prompt_template:
            prompt = PromptTemplate(
                input_variables=["context", "question", "image"],
                template=prompt_template,
            )
            final_prompt = prompt.format(context=context, question=query)
            contents[0] = final_prompt

        response = self.model.generate_content(contents=contents, stream=stream)
        return response.text

    def start_chat(self):
        self.chat_session = self.model.start_chat(history=[])
        return self.chat_session

    def send_chat_message(self, message: str, stream: bool = False):
        if not hasattr(self, 'chat_session'):
            raise ValueError("Chat session not started. Call start_chat() first.")
        response = self.chat_session.send_message(message, stream=stream)
        return response
    def get_collection_names(self):
        collection_names = self.qdrant_service.get_collections_names()
        return collection_names

    
    

##### Models ####
    
class Artist:
    def __init__(self, name: str = "NoArtistName", artist_biography: str = "NoArtistBiography", id: str = None):
        self.id = id if id else str(uuid.uuid4())
        self.name = name
        self.artist_biography = artist_biography

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'artist_biography': self.artist_biography
        }

    @classmethod
    def from_dict(cls, data):
        return cls(
            name=data.get('name', "NoArtistName"),
            artist_biography=data.get('artist_biography', "NoArtistBiography"),
            id=data.get('id')
        )

class Museum:
    def __init__(self, name: str = "NoMuseumName", museum_context: str = "NoMuseumContext", id: str = None):
        self.id = id if id else str(uuid.uuid4())
        self.name = name
        self.museum_context = museum_context

    def to_dict(self):
        return {
            'id': self.id,
            'name': self.name,
            'museum_context': self.museum_context
        }

    @classmethod
    def from_dict(cls, data):
        return cls(
            name=data.get('name', "NoMuseumName"),
            museum_context=data.get('museum_context', "NoMuseumContext"),
            id=data.get('id')
        )

class Artwork:
    def __init__(self, title: str = "NoTitle", artist: Artist = None, museum: Museum = None, ressources_path: str = "", id: str = None, description: str = ""):
        self.id = id if id else str(uuid.uuid4())
        self.title = title
        self.artist = artist if artist else Artist()
        self.museum = museum if museum else Museum()
        self.collection_name = f"{self.title}, {self.artist.name}"
        self.ressources_path = ressources_path
        self.description = description

    def to_dict(self):
        return {
            'id': self.id,
            'title': self.title,
            'artist': self.artist.to_dict(),
            'museum': self.museum.to_dict(),
            'collection_name': self.collection_name,
            'ressources_path': self.ressources_path,
            'description': self.description
        }

    @classmethod
    def from_dict(cls, data):
        artist = Artist.from_dict(data.get('artist', {}))
        museum = Museum.from_dict(data.get('museum', {}))
        return cls(
            title=data.get('title', "NoTitle"),
            artist=artist,
            museum=museum,
            ressources_path=data.get('ressources_path', ""),
            id=data.get('id'),
            description=data.get('description', "")
        )