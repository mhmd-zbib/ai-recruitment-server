# HireSync - AI Recruitment Platform

HireSync is an AI-powered recruitment management platform built with Spring Boot and PostgreSQL.

## Table of Contents

- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Setting Up Development Environment](#setting-up-development-environment)
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

### Setting Up Development Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/zbib/hiresync.git
   cd hiresync
   ```

2. Set up the environment:
   ```bash
   # Copy example environment file
   cp .env.example .env
   
   # Edit .env with appropriate values for your environment
   # Then run the setup command to initialize the project
   ./run setup
   ```

3. Start the application in development mode:
   ```bash
   ./run dev
   ```

4. Access the application:
   - Application: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Documentation: http://localhost:8080/v3/api-docs

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

## Building and Running

Build the application:
```bash
./run build
```

Build Docker image:
```bash
./run docker-build
```

Run the application with specific profile:
```bash
./run [dev|test|prod]
```

Database operations:
```bash
./run db start     # Start the database
./run db stop      # Stop the database
./run db status    # Check database status
./run db init      # Initialize schema
./run db backup    # Create a backup
```

## Deployment

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