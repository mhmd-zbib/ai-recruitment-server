# HireSync API

HireSync is an AI-enhanced recruitment pipeline backend application built with Spring Boot.

## Features

- User authentication and authorization with JWT
- Job management
- Application tracking
- Interview scheduling
- Comprehensive logging
- Error handling
- Multi-environment support (local, dev, prod)
- Docker-based deployment
- Automated CI/CD pipeline
- Code quality enforcement

## Prerequisites

- Java 17
- Maven
- Docker (for local development and deployment)
- Git

## Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hiresync.git
   cd hiresync
   ```

2. Copy the environment file:
   ```bash
   cp .env.example .env
   ```

3. Start local development:
   ```bash
   # On Unix/Linux/macOS
   ./run.sh local

   # On Windows
   run.cmd local
   ```

The application will be available at:
- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html

## Project Structure

```
hiresync/
├── .github/              # GitHub Actions workflows
│   └── workflows/        # CI/CD pipeline definitions
├── config/               # Project configuration files
│   ├── dependency-check-suppressions.xml  # OWASP dependency check suppressions
│   └── spotbugs-exclude.xml               # SpotBugs filter rules
├── docker/               # Docker related files
│   ├── docker-compose.yaml            # Docker Compose for local development
│   ├── docker-compose.prod.yaml       # Docker Compose for production
│   ├── Dockerfile                     # Main Dockerfile for production
│   ├── Dockerfile.fast                # Optimized Dockerfile for development
│   └── .dockerignore                  # Docker ignore file
├── scripts/              # Utility scripts
│   ├── build/           # Build-related scripts
│   ├── deploy/          # Deployment scripts
│   ├── dev/             # Development scripts
│   ├── quality/         # Quality check scripts
│   └── utils/           # Utility functions
├── src/                  # Source code
│   ├── main/            # Main application code
│   └── test/            # Test code
├── .env.example         # Example environment variables
├── run.sh              # Main entry script
└── run.cmd             # Main entry script (Windows)
```