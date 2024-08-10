# Interactive Audioguide Application

## Overview

I am excited to participate in the Gemini competition by Google and present my interactive audioguide application concept. The application aims to enhance the museum visitors' experience using AI technology. It consists of three modules:

1. **Backend**: Manages the vector store and handles calls to Gemini.
2. **Web Interface**: Allows artists and museums to interact with the Qdrant database.
3. **Android Frontend**: An AI-powered chatbot for museum visitors, enabling them to scan QR codes for more information.

I am keen on collaborating with Google to bring this concept to life.

## Project Structure

The project repository contains two main directories:

1. **frontend**: Contains the Android Studio project for the frontend.
2. **backend**: Contains the `main.py` file to launch the backend and the `ui` directory for the web interface.

## Application Modules

### Web Interface

To launch the web interface for adding artworks, museums, and artists, and generating QR codes, follow these steps:
1. Make sure you have Docker Desktop installed.
2. Pull the Qdrant image:
    ```bash
    docker pull qdrant/qdrant
    ```
    - [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
    - [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)
3. In your terminal, navigate to the `backend` directory and run:
    ```bash
    streamlit run ui/app.py
    ```

### Backend

To launch the backend, follow these steps:
1. Ensure you have the Qdrant image.
2. In your terminal, navigate to the `backend` directory and run the `main.py` script:
    ```bash
    python main.py
    ```

### Android Frontend

To launch the Android frontend:
1. Install Android Studio.
2. Obtain a Firebase key.
3. Open the project located in the `frontend` directory with Android Studio.

This module is a chatbot designed for museum visitors to interact with and scan QR codes for additional information.

## Acknowledgements

A special thanks to [GaneshajDivekar](https://github.com/GaneshajDivekar) and the [ChatGPTCompose_Lite](https://github.com/GaneshajDivekar/ChatGPTCompose_Lite) repository, which I utilized for the Android frontend development.

## Contact

For any further questions or collaboration inquiries, please feel free to contact me : ripeer.dev@gmail.com

Thank you for considering my application for the Gemini competition. I look forward to the opportunity to work with Google and advance this innovative project.
