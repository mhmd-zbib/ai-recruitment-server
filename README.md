# HireSync - AI Recruitment Platform

HireSync is an AI-powered recruitment management platform built with Spring Boot and PostgreSQL.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Setting Up Development Environment](#setting-up-development-environment)
  - [Local Development Setup](#local-development-setup)
- [Development Workflow](#development-workflow)
  - [Code Standards](#code-standards)
  - [Branch Strategy](#branch-strategy)
  - [Testing](#testing)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Building and Running](#building-and-running)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Getting Started

### Prerequisites

- JDK 17 or later
- Docker and Docker Compose
- Maven (or use the included Maven wrapper)
- Git
- Bash shell (included in Git Bash for Windows)

### Setting Up Development Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/zbib/hiresync.git
   cd hiresync
   ```

2. Set up the environment:
   ```bash
   # Make the hiresync script executable (Unix/macOS/Git Bash)
   chmod +x hiresync
   
   # Initialize the development environment
   ./hiresync setup
   ```

3. Start the application in development mode:
   ```bash
   ./hiresync start
   ```

4. Access the application:
   - Application: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Documentation: http://localhost:8080/v3/api-docs

### Local Development Setup

For a streamlined development experience, HireSync provides a unified script that automates all development tasks:

```bash
# Make the hiresync script executable (first time only)
chmod +x hiresync

# Show available commands
./hiresync help
```

Available commands:

- `./hiresync start`: Start local development environment (PostgreSQL + Spring Boot)
- `./hiresync stop`: Stop local development environment
- `./hiresync restart`: Restart local development environment
- `./hiresync status`: Check status of local development environment
- `./hiresync check`: Verify environment configuration
- `./hiresync logs`: View PostgreSQL container logs
- `./hiresync psql`: Open PostgreSQL interactive terminal
- `./hiresync run`: Run application directly (non-containerized) with .env variables
- `./hiresync postgres`: Start only PostgreSQL container (useful for IDE development)
- `./hiresync setup`: Initial environment setup (create .env, make scripts executable)
- `./hiresync clean`: Remove all data and start fresh (WARNING: destroys data)
- `./hiresync help`: Show help message

The automated setup ensures proper connection between the Spring Boot application and the PostgreSQL database.

For a detailed guide on setting up and using the local development environment, see [SETUP.md](SETUP.md).

#### Cross-Platform Support

The `hiresync` script is designed to work on Unix-based systems (Linux, macOS) and Windows:

**Unix-based systems (Linux, macOS):**
- Use the bash script directly: `./hiresync <command>`
- Make sure script is executable: `chmod +x hiresync`

**Windows:**
- Use Git Bash: Run the script as above
- Make sure Git Bash is installed (which includes bash)
- Run script with `./hiresync <command>` from Git Bash

## Development Workflow

### Code Standards

This project follows Google Java Style guidelines with the following quality checks:
- Checkstyle for code style validation
- SpotBugs for static code analysis
- PMD for additional code quality checks
- Spotless for code formatting

Before committing code, ensure it passes all quality checks:
```bash
./run quality  # To verify code quality
./run lint     # To fix formatting issues
```

### Branch Strategy

- `main`: Production-ready code
- `dev`: Integration branch for ongoing development
- Feature branches: Create from `dev` with the naming convention `feature/description`
- Bugfix branches: Create with the naming convention `bugfix/issue-id`

### Testing

Run tests with:
```bash
./run test
```

The project includes:
- Unit tests
- Integration tests
- API tests

## Project Structure

```
hiresync/
├── .github/          # GitHub workflows for CI/CD
├── .git-hooks/       # Git hooks for code quality enforcement
├── config/           # Code quality and security configurations
├── db/               # Database initialization scripts
├── docker/           # Docker and deployment configurations
├── scripts/          # Utility scripts for development and CI
├── src/              # Application source code
│   ├── main/
│   │   ├── java/     # Java source files
│   │   └── resources/# Application configuration and resources
│   └── test/         # Test sources
├── .env.example      # Example environment configuration
├── pom.xml           # Maven project configuration
├── hiresync          # Unified development environment script
└── run               # Command-line interface for common tasks
```

## Configuration

The application uses a layered configuration approach:
1. Default values in application.yaml
2. Environment-specific values in application-{env}.yaml
3. Runtime values from environment variables or .env file

Key configuration files:
- `src/main/resources/application.yaml`: Base configuration
- `src/main/resources/application-{env}.yaml`: Environment-specific configuration
- `.env`: Local environment variables (not committed to Git)

#### Container Restart Policies

You can configure how containers behave on system restart or after crashes by setting these environment variables in your `.env` file:

```
# Docker container restart policies
# Options: "no", "on-failure", "always", "unless-stopped"
DB_RESTART_POLICY=unless-stopped  # Database container restart policy
DEVTOOLS_RESTART_POLICY=no        # Development tools container restart policy
APP_RESTART_POLICY=always         # Application container restart policy (production)
```

Available restart policies:
- `no`: Never automatically restart the container
- `on-failure`: Restart only if the container exits with a non-zero status code
- `always`: Always restart the container regardless of exit status
- `unless-stopped`: Always restart the container unless it was explicitly stopped

Default policies:
- Development database: `unless-stopped` (survives restarts but can be manually stopped)
- Development tools: `no` (stops when Docker restarts)
- Production containers: `always` (always restarts, even after Docker restart)

## Building and Running

Build the application:
```bash
./run build
```

Build Docker image:
```bash
./run docker-build
```

The project includes GitHub Actions workflows for CI/CD:

- **CI Pipeline** (`ci.yml`): Runs on all branches to perform code quality checks, tests, and security scans
- **CD Pipeline** (`cd.yml`): Deploys to staging/production environments from main branch or tags
- **Release Pipeline** (`release.yml`): Creates new releases from tags

### Manual Deployment

For manual deployment to a server:

1. Set up environment:
   ```bash
   cp .env.example .env
   # Edit .env with production values
   ```

2. Deploy with Docker Compose:
   ```bash
   docker-compose -f docker/docker-compose.yaml -f docker/docker-compose.prod.yaml up -d
   ```

## Contributing

1. Ensure you have the Git hooks installed:
   ```bash
   ./run git-hooks
   ```

2. Create a feature branch from `dev`
3. Make your changes
4. Run tests and quality checks:
   ```bash
   ./run quality
   ./run test
   ```
5. Submit a pull request to the `dev` branch

## Development

### Running the Application

There are two ways to run the application:

#### 1. Using Containerized Environment (Recommended)

This method starts PostgreSQL in a container and runs the Spring Boot application in a development container:

```bash
./hiresync start
```

#### 2. Running Application Directly (Non-Containerized)

This method starts only the PostgreSQL in a container and runs the Spring Boot application directly on your system,
while still using the environment variables from your `.env` file:

```bash
# On Linux/macOS
./hiresync run

# On Windows
hiresync.cmd run
```

This is useful when you want to:
- Debug the application directly in your IDE
- Make code changes without restarting the container
- Have faster startup times during development

### Other Development Commands

### Environment Variable Injection

The HireSync development environment leverages `.env` files for configuration, making it easier to manage settings across different environments. When you run `hiresync start`, the following happens:

1. **Loading Environment Variables**: 
   - All variables from your `.env` file are loaded
   - Default values are applied for any missing variables
   - A temporary Docker-specific environment file is created

2. **Database Connection**: 
   - PostgreSQL is started with variables from your `.env` file (DB_USER, DB_PASSWORD, etc.)
   - Connection details are automatically configured based on these variables

3. **Application Configuration**:
   - An `application-env.yaml` file is generated with all settings from your `.env` file
   - The Spring Boot application is started with the `env` profile active alongside your specified profile
   - All environment variables are passed directly to the application

This approach ensures that:
- Your configuration is defined in one place (the `.env` file)
- Changes to configuration don't require modifying application code
- You can have different configurations for different environments (dev, test, prod)

Example `.env` variable usage:
```properties
# In .env file
DB_PORT=5544
DB_NAME=hiresync
LOG_LEVEL_APP=DEBUG

# These will be injected as:
# - Database container port mapping
# - Database name in PostgreSQL 
# - Logging level in Spring Boot application
```
