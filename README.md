# RV32I Emulator
*This project is purely for educational purpose*

## Docker Containerization

This project has been containerized using Docker to make it easy to deploy to cloud platforms like Render.

### Local Development with Docker

1. Build and run the application using Docker Compose:

```bash
docker-compose up --build
```

2. The WebSocket server will be available at `localhost:9092`

### Deploying to Render

1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Use the following settings:
   - Environment: Docker
   - Build Command: (leave empty, Render will use the Dockerfile)
   - Start Command: (leave empty, Render will use the CMD in Dockerfile)

4. Add the following environment variables if needed:
   - No specific environment variables are required as the application is configured to use "0.0.0.0" as hostname and port 9092

5. Click "Create Web Service"

### Important Notes

- The application is configured to listen on port 9092, which is exposed in the Docker configuration
- The application uses "0.0.0.0" as the hostname, which allows it to accept connections from any network interface
- The ELF files are stored in a volume mounted at `/app/elf_files`
