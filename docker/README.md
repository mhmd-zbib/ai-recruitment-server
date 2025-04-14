# Docker Configuration for HireSync

This directory contains Docker and container configurations for the HireSync application.

## Directory Structure

- `docker-compose.local.yaml`: Local development environment configuration
- `docker-compose.prod.yaml`: Production environment configuration
- `Dockerfile`: Application container build configuration

## Automated PostgreSQL Setup

The PostgreSQL container is configured to automatically set up the database environment based on variables defined in your `.env` file.

### How it works

PostgreSQL's official Docker image provides built-in automation:

1. When the container starts, it automatically:
   - Creates a PostgreSQL user with the credentials specified in `POSTGRES_USER` and `POSTGRES_PASSWORD`
   - Creates a database with the name specified in `POSTGRES_DB`
   - Sets the created user as the owner of the database

2. Our `docker-compose.local.yaml` file maps these directly to your `.env` variables:
   - `POSTGRES_USER: ${DB_USER:-hiresync}`
   - `POSTGRES_PASSWORD: ${DB_PASSWORD:-hiresync}`
   - `POSTGRES_DB: ${DB_NAME:-hiresync}`

### Environment Variables

The PostgreSQL initialization uses these variables from your `.env` file:

- `DB_NAME`: The name of the database to create (default: hiresync)
- `DB_USER`: The username to create (default: hiresync)
- `DB_PASSWORD`: The password for the user (default: hiresync)
- `DB_PORT`: The port mapping on the host (default: 5433)

### Container Access

- From host machine: `localhost:5433` (or your configured `DB_PORT`)
- From other containers: `postgres:5432` (internal network)

## Network Configuration

All containers are connected to a custom bridge network (`hiresync-network` by default) to enable container-to-container communication. 

# HireSync Docker Setup

This directory contains all Docker-related files needed to run HireSync in various environments.

## Running on Windows with Git Bash

When running on Windows with Git Bash, use these commands:

```bash
# From the project root directory:
./hiresync start

# If you encounter path issues, try running directly from the docker directory:
cd docker
./start-local.sh
```

## Troubleshooting

If you encounter issues with the volume mount (e.g., pom.xml not found):

1. Try debugging the container:
   ```bash
   ./hiresync debug
   ```

2. Try running directly from the docker directory:
   ```bash
   cd docker
   ./start-local.sh
   ```

3. If Docker Desktop file sharing isn't working properly:
   - Check Docker Desktop → Settings → Resources → File Sharing
   - Ensure your project directory is in a location Docker can access
   - Restart Docker Desktop

4. Windows path issues:
   ```bash
   # Manually modify the volume mount in docker-compose.local.yaml
   # Example: 
   # volumes:
   #   - /c/your/project/path:/app
   ```

## Available Commands

Use these commands from the project root:

```bash
./hiresync start      # Start the application
./hiresync stop       # Stop all containers
./hiresync restart    # Restart the application 
./hiresync logs       # View application logs
./hiresync shell      # Open a shell in the container
./hiresync debug      # Debug container mount issues
./hiresync quality    # Run code quality checks
./hiresync test       # Run tests
./hiresync build      # Build the application
```

## Alternative Manual Docker Commands

If the scripts don't work, you can use these Docker commands directly:

```bash
# Start containers
docker compose -f docker-compose.local.yaml up -d

# Check container filesystem
docker exec hiresync-devtools ls -la /app

# Run Spring Boot application
docker exec -it hiresync-devtools bash -c "cd /app && mvn spring-boot:run -Dspring-boot.run.profiles=local"

# Stop containers
docker compose -f docker-compose.local.yaml down
```

## Quick Start

The easiest way to work with Docker is using our `hiresync` script which handles all Docker operations:

```bash
# Start the application
./hiresync start

# Stop the application
./hiresync stop

# Restart the application
./hiresync restart

# View logs
./hiresync logs

# Open a shell in the development container
./hiresync shell
```

## Docker Compose Files

- `docker-compose.local.yaml` - For local development
- `docker-compose.dev.yaml` - For development environment
- `docker-compose.test.yaml` - For running tests
- `docker-compose.prod.yaml` - For production environment

## Manual Docker Usage

If you prefer to use Docker commands directly:

```bash
# Start containers
docker compose -f docker/docker-compose.local.yaml up -d

# Stop containers
docker compose -f docker/docker-compose.local.yaml down

# View logs
docker compose -f docker/docker-compose.local.yaml logs -f

# Run Spring Boot application in the container
docker exec -it hiresync-devtools bash -c 'cd /app && mvn spring-boot:run -Dspring-boot.run.profiles=local'

# Open a shell in the container
docker exec -it hiresync-devtools bash
```

## Windows-Specific Notes

When using Windows:

1. Use PowerShell or Git Bash for best compatibility
2. Make sure Docker Desktop is running with WSL2 backend for best performance
3. If using path mappings in Docker, use Linux-style paths (with forward slashes) 

## Custom Environment Variables

You can customize environment variables by:

1. Creating a `.env` file in the project root
2. Setting values in the `.env` file (using format `KEY=VALUE`)
3. These will be automatically loaded by both the `hiresync` script and Docker Compose 