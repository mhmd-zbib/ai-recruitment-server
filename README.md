# HireSync - AI-Powered Recruitment Platform

## Overview
HireSync is a modern recruitment platform that leverages AI to streamline the hiring process, making it more efficient and effective for both recruiters and candidates.

## Environment Setup

### Prerequisites
- Docker and Docker Compose
- Git
- Bash-compatible shell (Git Bash for Windows users)

### Quick Start
1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/hiresync.git
   cd hiresync
   ```

2. Configure environment variables:
   - Copy the sample environment file:
     ```bash
     cp .env.example .env
     ```
   - Edit the `.env` file to customize your settings (if needed)

3. Start the application:
   
   For local development with hot reloading:
   ```bash
   ./scripts/start-local.sh
   ```
   
   For production-like mode:
   ```bash
   ./scripts/start.sh
   ```

4. Access the application:
   - API: http://localhost:8080/api
   - Swagger UI: http://localhost:8080/api/swagger-ui.html

### Environment Variables
The application uses the following environment variables that you can customize in the `.env` file:

#### Application Configuration
- `SPRING_PROFILES_ACTIVE`: Application profile (local, dev, test, prod)
- `TZ`: Time zone (default: UTC)
- `APP_PORT`: Application port (default: 8080)
- `DEBUG_PORT`: Debug port for remote debugging (default: 5005)

#### Database Configuration
- `DB_PORT`: Database port mapped to host (default: 5433)
- `DB_HOST`: Database host (default: localhost)
- `DB_NAME`: Database name (default: hiresync)
- `DB_USER`: Database username (default: hiresync)
- `DB_PASSWORD`: Database password (default: hiresync)

#### Docker Resources
- `NETWORK_NAME`: Docker network name (default: hiresync-network)
- `POSTGRES_VOLUME`: PostgreSQL data volume name (default: hiresync-postgres-data)
- `MAVEN_REPO_VOLUME`: Maven repository volume name (default: hiresync-maven-repo)

#### Security Configuration
- `JWT_SECRET`: JWT signing secret (auto-generated for local development)
- `JWT_EXPIRATION`: JWT token validity in milliseconds (default: 86400000 - 24h)

### Usage

The project includes several convenience scripts in the `scripts` directory:

- `start.sh`: Start all services and the application in production-like mode
- `start-local.sh`: Start local development environment with hot reloading
- `stop.sh`: Stop all services
- `status.sh`: Show status of services
- `app.sh`: Start only the Spring Boot application
- `services.sh`: Start only the supporting services (PostgreSQL, etc.)
- `clean.sh`: Stop services and remove volumes (data reset)
- `lint.sh`: Run comprehensive code quality and linting checks
- `help.sh`: Show help information

## Development

### Local Development Environment

The `start-local.sh` script sets up a complete development environment with the following features:

- **Hot reloading**: Automatically reloads the application when Java files change
- **Environment configuration**: Sets up development-specific environment variables
- **Service management**: Starts and orchestrates Docker containers in the right order
- **Database initialization**: Manages database migrations and development data seeding
- **Health monitoring**: Monitors the status of running services

To use additional development features, set the following environment variables before running the script:

- `AUTO_MIGRATE=true`: Automatically run database migrations
- `SEED_DEV_DATA=true`: Seed the database with development data
- `PULL_LATEST=true`: Pull the latest Docker images before starting

Example:
```bash
AUTO_MIGRATE=true SEED_DEV_DATA=true ./scripts/start-local.sh
```

### Code Quality

Run linting and code quality checks with:

```bash
./scripts/lint.sh
```

This will perform various checks including:
- Checkstyle validation
- SpotBugs static analysis
- Code duplication detection
- Code formatting verification

### Database Management
The PostgreSQL database will be automatically created on first startup with the credentials specified in your `.env` file. If you change these credentials after the initial setup, you'll need to run `./scripts/clean.sh` to remove the existing database volume and recreate it with the new credentials.

### Application Structure
- `/src/main`: Main application source code
- `/src/test`: Test source code
- `/docker`: Docker configuration files
- `/scripts`: Utility scripts for development and deployment

## Production Deployment
For production deployment, use the production Docker Compose file:

```bash
docker-compose -f docker/docker-compose.prod.yaml up -d
```

Ensure you set appropriate secure values for all environment variables in a production environment.

## License
Copyright (c) 2025 HireSync. All rights reserved.
