import streamlit as st
from PIL import Image

import logging
from models import (
    Artist,
    Museum,
    Artwork,
    initialize_firestore,
    add_artist_firebase,
    add_museum_firebase,
    add_artwork_firebase,
    get_artist_biography,
    get_artist_by_name,
    get_museum_by_name,
    get_all_artists,
    get_all_museums,
    get_description_from_gemini,
    create_collection,
    generate_qr_code
)
from embedding.embedding_service import EmbeddingService
from vectorstore.qdrant_service import QdrantService
from gemini.gemini_service import GeminiService
import os

# Custom CSS for better styling
st.markdown("""
    <style>
        .main-title {
            font-size: 48px;
            color: #6650a4;
            text-align: center;
            font-weight: bold;
            margin-top: 50px;
        }
        .header-title {
            font-size: 36px;
            color: #D0BCFF;
            margin-top: 20px;
            text-align: center;
        }
        .description-text {
            font-size: 20px;
            color: #9E9E9E;
        }
        .input-field {
            margin-top: 10px;
        }
        .footer {
            margin-top: 50px;
            text-align: center;
            font-size: 16px;
            color: #888;
        }
        .button {
            margin-top: 20px;
        }
    </style>
""", unsafe_allow_html=True)

# Configure logging
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

# Initialize Firestore
db = initialize_firestore()

@st.cache_resource
def load_embedding_service():
    return EmbeddingService()

@st.cache_resource
def load_qdrant_service(_embedding_service):
    return QdrantService(embedding_service=_embedding_service)

@st.cache_resource
def load_gemini_service(_qdrant_service):
    return GeminiService(qdrant_service=_qdrant_service)

# Initialize services
embedding_service = load_embedding_service()
qdrant_service = load_qdrant_service(embedding_service)
gemini_service = load_gemini_service(qdrant_service)

# Add main title
st.markdown('<p class="main-title">Artalk Web Interface for Artists</p>', unsafe_allow_html=True)

st.markdown('<p class="header-title">Add Artists, Museums, and Artworks</p>', unsafe_allow_html=True)

# Function to fetch all artists with caching
def fetch_all_artists(_db):
    return get_all_artists(_db)

# Function to fetch all museums with caching
def fetch_all_museums(_db):
    return get_all_museums(_db)

# Function to resize image
def resize_image(image, max_size=(800, 800)):
    img = Image.open(image)
    img.thumbnail(max_size)
    return img

# Initialize session state for checking page reload
if 'page_reloaded' not in st.session_state:
    st.session_state.page_reloaded = False

# Fetch the list of artists and museums
if not st.session_state.page_reloaded:
    all_artists = fetch_all_artists(db)
    all_museums = fetch_all_museums(db)
    st.session_state.all_artists = all_artists
    st.session_state.all_museums = all_museums
    st.session_state.page_reloaded = True
    logging.debug(f"Fetched artists: {all_artists}")
    logging.debug(f"Fetched museums: {all_museums}")
else:
    all_artists = st.session_state.all_artists
    all_museums = st.session_state.all_museums

# Convert objects to list of names for dropdown menus
artist_names = [""] + [artist.name for artist in all_artists]
museum_names = [""] + [museum.name for museum in all_museums]

# Initialize values in st.session_state if not already set
for key in ['artwork_title', 'artwork_artist_name', 'artwork_museum_name', 'artwork_description_where',
            'artwork_description_when', 'artwork_description_who', 'artwork_description_how',
            'artwork_description_why', 'description', 'artist_name', 'artist_biography', 'museum_name',
            'museum_context', 'pdf_path']:
    if key not in st.session_state:
        st.session_state[key] = ""

# Add an artwork section
st.markdown('<p class="header-title">Add an Artwork</p>', unsafe_allow_html=True)
st.session_state.artwork_title = st.text_input("Artwork Title", value=st.session_state.artwork_title)
st.session_state.artwork_artist_name = st.selectbox("Artist Name", artist_names, index=artist_names.index(st.session_state.artwork_artist_name) if st.session_state.artwork_artist_name in artist_names else 0)
st.session_state.artwork_museum_name = st.selectbox("Museum Name", museum_names, index=museum_names.index(st.session_state.artwork_museum_name) if st.session_state.artwork_museum_name in museum_names else 0)

# File uploader for PDFs
uploaded_pdfs = st.file_uploader("Artwork Resources (PDF)", type=["pdf"], accept_multiple_files=True)
if uploaded_pdfs:
    first_pdf_name = uploaded_pdfs[0].name
    folder_name = first_pdf_name[:10]
    save_directory = os.path.join(r"D:\Boulot\Intelligence_Artificielle\gemini_app\backend\ui\data\pdfs", folder_name)
    if not os.path.exists(save_directory):
        os.makedirs(save_directory)
    for pdf_file in uploaded_pdfs:
        pdf_path = os.path.join(save_directory, pdf_file.name)
        with open(pdf_path, "wb") as f:
            f.write(pdf_file.getbuffer())
    st.session_state.pdf_path = save_directory
    st.success("PDFs uploaded successfully")

# File uploader for image
uploaded_file = st.file_uploader("Upload Artwork Image", type=["png", "jpg", "jpeg"])
if uploaded_file:
    try:
        if not uploaded_file.type.lower() in ["image/png", "image/jpeg"]:
            st.error("Please upload an image file (png, jpg, jpeg).")
        else:
            # Resize the image
            img = resize_image(uploaded_file)
            
            # Save the resized image in the specified directory
            save_directory = r"D:\Boulot\Intelligence_Artificielle\gemini_app\backend\ui\data\images"
            if not os.path.exists(save_directory):
                os.makedirs(save_directory)
            image_path = os.path.join(save_directory, uploaded_file.name)
            img.save(image_path)
            logging.debug(f"Image saved at {image_path}")

            # Simulate a call to the Gemini API for image description
            st.session_state.description = get_description_from_gemini(image_path)
            logging.debug(f"Description from Gemini: {st.session_state.description}")
            st.success("Image uploaded, resized, and described successfully")

    except Exception as e:
        logging.error(f"Error processing the image: {e}")
        st.error(f"Error processing the image: {e}")
        st.session_state.description = ""

# Display the generated or existing description
st.markdown('<div class="description-text">', unsafe_allow_html=True)
st.text_area("Generated Description by Gemini", st.session_state.description, height=200)
st.markdown('</div>', unsafe_allow_html=True)

# Questions for the description
st.markdown('<p class="header-title">Artwork Description</p>', unsafe_allow_html=True)
st.session_state.artwork_description_where = st.text_area("Where?", st.session_state.artwork_description_where, height=100)
st.session_state.artwork_description_when = st.text_area("When?", st.session_state.artwork_description_when, height=100)
st.session_state.artwork_description_who = st.text_area("Who?", st.session_state.artwork_description_who, height=100)
st.session_state.artwork_description_how = st.text_area("How?", st.session_state.artwork_description_how, height=100)
st.session_state.artwork_description_why = st.text_area("Why?", st.session_state.artwork_description_why, height=100)

if st.button("Add Artwork"):
    description = f"{st.session_state.description}\n\nWhere: {st.session_state.artwork_description_where}\nWhen: {st.session_state.artwork_description_when}\nWho: {st.session_state.artwork_description_who}\nHow: {st.session_state.artwork_description_how}\nWhy: {st.session_state.artwork_description_why}"
    try:
        artist = get_artist_by_name(db, st.session_state.artwork_artist_name) if st.session_state.artwork_artist_name else None
        museum = get_museum_by_name(db, st.session_state.artwork_museum_name) if st.session_state.artwork_museum_name else None
        artwork = Artwork(
            title=st.session_state.artwork_title,
            artist=artist,
            museum=museum,
            description=description,
            ressources_path=st.session_state.pdf_path if 'pdf_path' in st.session_state else ""
        )
        add_artwork_firebase(db, artwork)
        create_collection(pdf_path=st.session_state.pdf_path, artwork=artwork)
        qr_code_path = generate_qr_code(artwork=artwork)
        st.success(f"Artwork '{st.session_state.artwork_title}' added successfully.")
        logging.debug(f"Artwork added: {artwork.to_dict()}")
        
        # Display the QR code
        st.image(qr_code_path, caption="Artwork QR Code")
        # Button to download the QR code
        with open(qr_code_path, "rb") as file:
            btn = st.download_button(
                label="Download QR Code",
                data=file,
                file_name=os.path.basename(qr_code_path),
                mime="image/png"
            )
        
        # Reset all fields after success
        for key in ['artwork_title', 'artwork_artist_name', 'artwork_museum_name', 'artwork_description_where',
                    'artwork_description_when', 'artwork_description_who', 'artwork_description_how',
                    'artwork_description_why', 'description', 'pdf_path']:
            st.session_state[key] = ""

    except ValueError as e:
        logging.error(f"Error adding artwork: {e}")
        st.error(e)

# Add an artist section
st.markdown('<p class="header-title">Add an Artist</p>', unsafe_allow_html=True)
st.session_state.artist_name = st.text_input("Artist Name", value=st.session_state.artist_name)
st.session_state.artist_biography = st.text_input("Artist Biography", value=st.session_state.artist_biography)

if st.button("Add Artist"):
    try:
        artist = Artist(name=st.session_state.artist_name, artist_biography=st.session_state.artist_biography)
        add_artist_firebase(db, artist)
        st.success(f"Artist '{st.session_state.artist_name}' added successfully.")
        logging.debug(f"Artist added: {artist.to_dict()}")
        
        # Reset fields after success
        st.session_state.artist_name = ""
        st.session_state.artist_biography = ""

    except Exception as e:
        logging.error(f"Error adding artist: {e}")
        st.error(f"Error adding artist: {e}")

# Add a museum section
st.markdown('<p class="header-title">Add a Museum</p>', unsafe_allow_html=True)
st.session_state.museum_name = st.text_input("Museum Name", value=st.session_state.museum_name)
st.session_state.museum_context = st.text_area("Museum Context", st.session_state.museum_context, height=200)

if st.button("Add Museum"):
    try:
        museum = Museum(name=st.session_state.museum_name, museum_context=st.session_state.museum_context)
        add_museum_firebase(db, museum)
        st.success(f"Museum '{st.session_state.museum_name}' added successfully.")
        logging.debug(f"Museum added: {museum.to_dict()}")
        
        # Reset fields after success
        st.session_state.museum_name = ""
        st.session_state.museum_context = ""

    except Exception as e:
        logging.error(f"Error adding museum: {e}")
        st.error(f"Error adding museum: {e}")

# Footer
st.markdown('<div class="footer">Powered by Artalk</div>', unsafe_allow_html=True)