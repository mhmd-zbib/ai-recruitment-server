# HireSync API

HireSync is an AI-enhanced recruitment pipeline backend application built with Spring Boot.

## Features

- User authentication and authorization with JWT
- Job management
- Application tracking
- Interview scheduling
- Comprehensive logging
- Error handling

## Environment Configuration

HireSync uses environment variables for configuration. A `.env` file is used to manage these variables:

1. Copy the sample environment file:
   ```
   cp .env.example .env
   ```

2. Edit the `.env` file with your configuration values.

3. The application supports three main environment profiles:
   - `local`: For local development with Docker PostgreSQL database or H2 in-memory database
   - `dev`: For development/staging environments
   - `prod`: For production deployment

## Project Structure

The project follows a clean, organized structure:

```
hiresync/
├── .github/              # GitHub Actions workflows
│   └── workflows/        # CI/CD pipeline definitions 
├── scripts/              # Utility scripts
│   ├── ci-cd/            # CI/CD related scripts
│   ├── dev/              # Development environment scripts
│   ├── docker/           # Docker build scripts
│   ├── local/            # Local development scripts
│   ├── prod/             # Production deployment scripts
│   └── utils/            # Shared utility functions
├── src/                  # Source code
│   ├── main/             # Main application code
│   └── test/             # Test code
├── .env.example          # Example environment variables
├── docker-compose.yaml   # Docker Compose config for local development
├── docker-compose.prod.yaml # Docker Compose config for production
├── Dockerfile            # Main Dockerfile for production builds
├── Dockerfile.fast       # Optimized Dockerfile for development
└── run.sh                # Main entry script for all commands
```

## Running the Application

We provide a unified shell script interface to run the application in different modes:

```bash
# Make scripts executable
chmod +x run.sh
chmod +x scripts/**/*.sh

# Start in local mode (auto-detects Docker, falls back to H2 if not available)
./run.sh local

# Force using H2 database even if Docker is running
./run.sh local --h2

# Start in development mode (requires Docker)
./run.sh dev

# Build Docker image
./run.sh docker

# Run code verification
./run.sh verify

# Run tests
./run.sh test
```

## Production Deployment

To deploy the application to production, use:

```bash
# Deploy with Docker Compose
./run.sh prod --docker

# Deploy as standalone JAR (requires environment variables)
./run.sh prod --jar
```

Make sure to set the required environment variables for production deployment:
- `JDBC_DATABASE_URL`
- `JDBC_DATABASE_USERNAME` 
- `JDBC_DATABASE_PASSWORD`

## Prerequisites

- Java 17
- Maven
- PostgreSQL
- Docker (optional)

## Git Hooks

This project uses Git hooks to ensure code quality before commits:

### Automatic Setup (Recommended)

The hooks are automatically installed when you run:
```
mvn initialize
```

This happens because we use the `githook-maven-plugin` which installs the hooks from the `.git-hooks` directory to your local `.git/hooks` directory.

### What the Hooks Do

#### Pre-commit Hook
- Formats code with Spotless
- Checks code style with Checkstyle
- Runs PMD for static code analysis
- Prevents committing code with style issues or unused imports

#### Commit-msg Hook
- Enforces [Conventional Commits](https://www.conventionalcommits.org/) format
- Requires commit messages to follow the pattern: `type(scope): subject`
- Valid types: feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert
- Examples:
  - `feat(user): add login functionality`
  - `fix(auth): resolve token validation issue`
  - `docs(readme): update installation instructions`

### Manual Setup (If Needed)

If the hooks aren't working, you can install them manually:

#### Windows:
```
.\.git-hooks\install-hooks.bat
```

#### Unix/Linux/MacOS:
```
chmod +x ./.git-hooks/install-hooks.sh
./.git-hooks/install-hooks.sh
```

## Local Development

### Option 1: Using connect-db.sh (Recommended)

This smart script automatically detects if Docker is running and configures the application accordingly:

1. Make the script executable:
   ```bash
   chmod +x connect-db.sh
   ```

2. Run the application:
   ```bash
   ./connect-db.sh
   ```

This script will:
- Check if Docker is running
- If Docker is available, configure the application to use PostgreSQL and start the Docker container
- If Docker is not available, configure the application to use H2 in-memory database
- Start the application with the appropriate configuration

### Option 2: Using a Specific Environment

You can also specify which environment to use:

```bash
# Make scripts executable
chmod +x connect-db.sh run-dev.sh run-prod.sh

# For local environment (auto-detects Docker)
./connect-db.sh --env=local

# For development environment (with Docker PostgreSQL)
./run-dev.sh

# For production environment (with external database)
# Make sure to set JDBC_DATABASE_URL, JDBC_DATABASE_USERNAME, and JDBC_DATABASE_PASSWORD
./run-prod.sh
```

### Option 3: Fully Containerized (Production-like)

1. Make sure Docker is running
2. Create your `.env` file from the `.env.example`
3. Run the application:
```
docker-compose -f docker-compose.prod.yaml up -d
```
This will:
- Start PostgreSQL and the application in containers
- Run everything in a Docker network
- Use the production profile

## Deploying to Render

### Method 1: Deploy from GitHub

1. Push your code to a GitHub repository
2. Create an account on [Render](https://render.com/)
3. Click **New** and select **Web Service**
4. Choose **Build and deploy from a Git repository**
5. Connect your GitHub account and select your repository
6. Configure the service:
   - **Name**: hiresync-api
   - **Region**: Choose the closest to your users
   - **Branch**: main (or your deployment branch)
   - **Runtime**: Docker
   - **Plan**: Free tier (or select a paid plan for production)
7. Set environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `JDBC_DATABASE_URL=postgres://...` (Get this from your Render PostgreSQL instance)
   - `JDBC_DATABASE_USERNAME=...`
   - `JDBC_DATABASE_PASSWORD=...`
   - `JWT_SECRET=...` (Generate a secure random string)
8. Click **Create Web Service**

### Method 2: Deploy from Docker Hub

1. Build your Docker image:
```
docker build -t yourusername/hiresync:latest .
```

2. Push to Docker Hub:
```
docker login
docker push yourusername/hiresync:latest
```

3. On Render:
   - Go to your profile settings
   - Enable **Early Access -> Deploy from external registries**
   - Create a new Web Service
   - Select **Deploy an existing image from a registry**
   - Enter your Docker Hub image URL
   - Configure environment variables as above
   - Deploy the service

## Setting Up a Database on Render

1. On Render dashboard, click **New** and select **PostgreSQL**
2. Configure your database:
   - **Name**: hiresync-db
   - **Database**: hiresync_db
   - **User**: hiresync_user
   - Select your preferred region and plan
3. After creation, use the Internal Database URL in your web service environment variables

## API Documentation

Once deployed, API documentation is available at:
- Locally: `http://localhost:8080/api/swagger-ui.html`
- On Render: `https://your-service-name.onrender.com/api/swagger-ui.html`

## CI/CD Pipeline

HireSync uses GitHub Actions for continuous integration and continuous deployment with a multi-environment strategy.

### Pipeline Overview

Our CI/CD pipeline consists of two parts:

1. **Continuous Integration (CI)** - `.github/workflows/ci.yml`:
   - Code checkout and dependency setup
   - Code formatting with Spotless
   - Style checks with Checkstyle and PMD
   - Unit and integration tests
   - Code quality analysis with SonarCloud
   - Build and package the application
   - Upload build artifacts

2. **Continuous Deployment (CD)** - `.github/workflows/cd.yml`:
   - Branch-based deployment to different environments:
     - `dev` branch → Railway (Development)
     - `master` branch → Render (Production)
   - Docker image building and pushing to Docker Hub
   - Automated deployment to the appropriate platform
   - Deployment verification with health checks
   - Notifications of deployment status

### Environment Strategy

We use a multi-environment deployment strategy:

- **Development (Railway)**
  - Connected to the `dev` branch
  - Automatic deployment on successful CI builds
  - Used for feature testing and integration

- **Production (Render)**
  - Connected to the `master` branch
  - Automatic deployment on successful CI builds
  - Used for the live, customer-facing application

### Required Secrets

To use the CI/CD pipeline, you need to set up the following secrets in your GitHub repository:

- Docker Hub credentials:
  - `DOCKER_USERNAME`: Your Docker Hub username
  - `DOCKER_PASSWORD`: Your Docker Hub password

- Railway deployment:
  - `RAILWAY_TOKEN`: API token for Railway
  - `RAILWAY_APP_URL`: URL of your Railway application

- Render deployment:
  - `RENDER_DEPLOY_HOOK_URL`: Webhook URL for Render deployment
  - `RENDER_APP_URL`: URL of your Render application

- SonarCloud integration:
  - `