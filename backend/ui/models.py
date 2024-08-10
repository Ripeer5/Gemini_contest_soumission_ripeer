import uuid
import os
import qrcode
import wikipediaapi
from google.cloud import firestore
from embedding.embedding_service import EmbeddingService
from vectorstore.qdrant_service import QdrantService
from gemini.gemini_service import GeminiService
from chunking.chunking_service import ChunkingService

from prompt.prompt_library import prompt_description_artwork_gemini_query

# Initialisation des services
embedding_service = EmbeddingService()
chunking_service = ChunkingService()
qdrant_service = QdrantService(embedding_service=embedding_service)
gemini_service = GeminiService(qdrant_service=qdrant_service)

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


def generate_qr_code(artwork: Artwork):
    # Générer le QR code
    qr = qrcode.QRCode(
        version=1,  # Version du QR code, plus le nombre est élevé plus le QR code peut stocker de données
        error_correction=qrcode.constants.ERROR_CORRECT_L,  # Niveau de correction d'erreurs
        box_size=10,  # Taille de chaque boîte (pixel) du QR code
        border=4,  # Taille de la bordure (en boîtes)
    )
    qr.add_data(artwork.id)
    qr.make(fit=True)

    # Créer une image du QR code
    img = qr.make_image(fill_color="black", back_color="white")
    data_folder_path = "./data/qrcode/"
    qr_code_path = data_folder_path + artwork.title + "_" + artwork.artist.name + ".png"
    # Vérifier si le répertoire existe, sinon le créer
    os.makedirs(os.path.dirname(qr_code_path), exist_ok=True)

    # Enregistrer l'image dans un fichier
    img.save(qr_code_path)

    print(f"QR code généré et enregistré sous '{qr_code_path}'")
    return qr_code_path


def create_collection(pdf_path, artwork: Artwork):
    try:
        # Get the chunked documents
        documents = chunking_service.get_chunked_documents_from_pdf(path=pdf_path )
        # Save documents
        collection_name = artwork.title + ", " + artwork.artist.name
        # chunking_service.save_chunked_documents_to_pickle(file_path=pdf_path, documents=documents, file_name=collection_name)
        qdrant_service.create_collection(docs=documents, collection_name=collection_name)
    except Exception as e:
        print(f"Erreur lors de la création de la collection : {e}")
    
def get_description_from_gemini(image_path):
    query = prompt_description_artwork_gemini_query
    description = gemini_service.get_response(query=query, context="Pas de contexte", image_path=image_path)
    return description
def initialize_firestore():
    db = firestore.Client()
    return db

def update_document_if_exists(doc_ref, new_data):
    existing_doc = doc_ref.get()
    if existing_doc.exists:
        existing_data = existing_doc.to_dict()
        updated_data = {key: new_data[key] for key in new_data if new_data[key] != existing_data.get(key)}
        if updated_data:
            doc_ref.update(updated_data)
            print(f"Document mis à jour avec les changements: {updated_data}")
        else:
            print("Aucun changement détecté, document non mis à jour.")
    else:
        doc_ref.set(new_data)
        print("Nouveau document créé.")

def add_artist_firebase(db, artist):
    try:
        doc_ref = db.collection('artists').document(artist.name)
        existing_doc = doc_ref.get()
        if existing_doc.exists:
            existing_artist = Artist.from_dict(existing_doc.to_dict())
            existing_artist.artist_biography = artist.artist_biography
            doc_ref.set(existing_artist.to_dict())
            print(f"Biographie de l'artiste {artist.name} mise à jour.")
        else:
            doc_ref.set(artist.to_dict())
            print(f"Artiste {artist.name} ajouté avec succès.")
    except Exception as e:
        print(f"Erreur lors de l'ajout de l'artiste dans firebase : {e}")

def add_museum_firebase(db, museum):
    try:
        doc_ref = db.collection('museums').document(museum.name)
        existing_doc = doc_ref.get()
        if existing_doc.exists:
            existing_museum = Museum.from_dict(existing_doc.to_dict())
            existing_museum.museum_context = museum.museum_context
            doc_ref.set(existing_museum.to_dict())
            print(f"Contexte du musée {museum.name} mis à jour.")
        else:
            doc_ref.set(museum.to_dict())
            print(f"Musée {museum.name} ajouté avec succès.")
    except Exception as e:
        print(f"Erreur lors de l'ajout du musée dans firebase : {e}")

def add_artwork_firebase(db, artwork):
    try:
        doc_ref = db.collection('artworks').document(artwork.id)
        update_document_if_exists(doc_ref, artwork.to_dict())
    except Exception as e:
        print(f"Erreur lors de l'ajout de l'Artwork dans firebase : {e}")

def get_artist_biography(artist_name):
    wiki_wiki = wikipediaapi.Wikipedia('en')
    page = wiki_wiki.page(artist_name)
    if page.exists:
        return page.summary
    else:
        return "Biographie non trouvée"

def get_artist_by_name(db, artist_name):
    doc_ref = db.collection('artists').document(artist_name)
    doc = doc_ref.get()
    if doc.exists:
        return Artist.from_dict(doc.to_dict())
    else:
        raise ValueError(f"Artist with name {artist_name} not found")

def get_museum_by_name(db, museum_name):
    doc_ref = db.collection('museums').document(museum_name)
    doc = doc_ref.get()
    if doc.exists:
        return Museum.from_dict(doc.to_dict())
    else:
        raise ValueError(f"Museum with name {museum_name} not found")

def get_all_artists(db):
    artists_ref = db.collection('artists')
    docs = artists_ref.stream()
    return [Artist.from_dict(doc.to_dict()) for doc in docs]

def get_all_museums(db):
    museums_ref = db.collection('museums')
    docs = museums_ref.stream()
    return [Museum.from_dict(doc.to_dict()) for doc in docs]
