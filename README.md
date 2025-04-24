# HireSync - AI-Powered Recruitment Platform

HireSync is a comprehensive recruitment management platform that simplifies and enhances the hiring process with AI assistance.

## Quick Start

The application can be controlled using the centralized `hiresync` command:

```bash
# Start the application in production mode
./hiresync start

# Start in local development mode with hot reload
./hiresync local

# View service status
./hiresync status

# Stop all services
./hiresync stop
```

## Available Commands

Run `./hiresync help` to see all available commands:

```
Commands:
  start       Start the application in production mode
  local       Start in local development mode with hot reload
  app         Run the application without services
  services    Manage supporting services
  status      Show status of services
  stop        Stop all services
  restart     Restart all services
  clean       Stop services and clean data
  lint        Run code linting
  quality     Run all quality checks
  help        Show this help message
```

## Development

For development, use the local mode with additional options:

```bash
# Start in local mode with database migrations
./hiresync local --migrate

# Enable remote debugging
./hiresync local --debug

# Start without services (if you're running them separately)
./hiresync local --no-services
```

## Advanced Usage

For managing services separately:

```bash
# Start just the services (database, etc.)
./hiresync services start

# View service logs
./hiresync services logs

# Clean up data volumes (WARNING: removes all data)
./hiresync services clean
```

For running the application with specific profiles:

```bash
# Run with production profile
./hiresync app prod

# Run with test profile and debugging enabled
./hiresync app test --debug
```

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
   ./hiresync local
   ```
   
   For production-like mode:
   ```bash
   ./hiresync start
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

The project uses a unified command interface through the `hiresync` script:

- `./hiresync start`: Start all services and the application in production mode
- `./hiresync local`: Start local development environment with hot reloading
- `./hiresync stop`: Stop all services
- `./hiresync status`: Show status of services
- `./hiresync app`: Start only the Spring Boot application
- `./hiresync services`: Manage supporting services (PostgreSQL, etc.)
- `./hiresync clean`: Stop services and remove volumes (data reset)
- `./hiresync lint`: Run comprehensive code quality and linting checks
- `./hiresync help`: Show help information

## Development

### Local Development Environment

The `./hiresync local` command sets up a complete development environment with the following features:

- **Hot reloading**: Automatically reloads the application when Java files change
- **Environment configuration**: Sets up development-specific environment variables
- **Service management**: Starts and orchestrates Docker containers in the right order
- **Database initialization**: Manages database migrations and development data seeding
- **Health monitoring**: Monitors the status of running services

To use additional development features, use command-line flags:

```bash
# Run database migrations
./hiresync local --migrate

# Seed the database with development data
./hiresync local --seed

# Run with both migrations and data seeding
./hiresync local --migrate --seed

# Enable remote debugging
./hiresync local --debug
```

### Code Quality

Run linting and code quality checks with:

```bash
./hiresync lint
```

This will perform various checks including:
- Checkstyle validation
- SpotBugs static analysis
- Code duplication detection

## Continuous Integration & Deployment

HireSync uses GitHub Actions for CI/CD to automate testing, building, and deployment processes.

### CI Pipeline

The CI pipeline runs automatically on:
- Every push to `master` and `dev` branches
- Every pull request to these branches

It performs the following tasks:
- Code quality validation
- Unit and integration tests
- Building and packaging the application
- Building and pushing Docker images to Docker Hub

### CD Pipeline

The CD pipeline automatically deploys the application to different environments:

| Branch | Environment | Hosting Provider |
|--------|-------------|------------------|
| `dev`  | Development | Digital Ocean    |
| `master` | Production | AWS EC2         |

Deployments are fully automated - when code is merged to either branch, it is:
1. Built and tested in the CI pipeline
2. Packaged as a Docker image and pushed to Docker Hub
3. Deployed to the corresponding environment

### Manual Deployments

To manually trigger a deployment:

1. Go to the 'Actions' tab in the GitHub repository
2. Select the 'CD Pipeline' workflow
3. Click 'Run workflow'
4. Select the branch and environment
5. Click 'Run workflow'

### Environment Setup

For detailed instructions on setting up environments for deployment, see [CD Environment Setup](docs/cd-environment-setup.md).

### Server Setup

To set up a new server for deployment:

```bash
# For development environment
./scripts/server-setup.sh dev

# For production environment
./scripts/server-setup.sh prod
```

## License
Copyright (c) 2025 HireSync. All rights reserved.

## Development Scripts

HireSync includes a comprehensive set of development scripts to help manage the application lifecycle.

### Main Commands

The main entry point is the `hiresync` script in the root directory, which provides access to all functionality:

```bash
# View help and available commands
./hiresync help

# Application commands
./hiresync app start     # Start in production mode
./hiresync app dev       # Start in development mode with hot reload
./hiresync app test      # Start in test mode
./hiresync app stop      # Stop the application

# Build and deployment commands
./hiresync build package # Build application package
./hiresync build docker  # Build Docker image
./hiresync build deploy  # Deploy application

# Service management
./hiresync services start  # Start services
./hiresync services stop   # Stop services
./hiresync services status # View service status

# Database operations
./hiresync database migrate # Run database migrations
./hiresync database backup  # Backup database
./hiresync database seed    # Seed database with data

# Code quality
./hiresync quality test     # Run tests
./hiresync quality lint     # Check code style
./hiresync quality format   # Format code
```

### Script Organization

Scripts are organized in a modular structure:

```
scripts/
├── common/           # Common utilities
│   ├── app.sh        # Application operations
│   ├── docker.sh     # Docker operations
│   ├── env.sh        # Environment setup
│   ├── init.sh       # Script initialization
│   ├── logging.sh    # Logging utilities
│   └── utils.sh      # Common utilities
├── core/             # Core application scripts
│   ├── app.sh        # Application runtime management
│   ├── build.sh      # Build and deployment operations
│   ├── quality.sh    # Code quality operations
│   └── services.sh   # Service management
└── database/         # Database operations
    ├── backup.sh     # Database backup/restore
    ├── db.sh         # Main database script
    ├── migrate.sh    # Database migrations
    └── seed.sh       # Data seeding
```

## Git Hooks

This project uses Git hooks to ensure code quality before commits. To install the hooks:

### On Unix/Linux/MacOS:
```bash
# Make the scripts executable
chmod +x .git-hooks/install-hooks.sh .git-hooks/pre-commit

# Install the hooks
./.git-hooks/install-hooks.sh
```

### On Windows:
```powershell
# Install the hooks using PowerShell
# This will create a symlink to the pre-commit hook
git config core.hooksPath .git-hooks
```

The pre-commit hook performs the following lightweight checks:
- Checks for merge conflicts
- Detects large files (>500KB)
- Validates commit message format (conventional commits)
- Compiles the code
- Runs checkstyle on changed files only
- Runs PMD rules on changed files only using the main ruleset

To bypass the pre-commit hook temporarily, use:
```bash
SKIP_HOOKS=1 git commit
# or on Windows
set SKIP_HOOKS=1 && git commit
```

## Code Quality Checks

The project includes a comprehensive script for running code quality checks and formatting.

### Quality Check Script

Run the quality check script to ensure your code meets the project's quality standards:

```bash
./scripts/quality-check.sh
```

The script performs the following checks in sequence:

1. **Code Formatting (Spotless)**: Ensures consistent code style
2. **PMD**: Static code analysis to find code smells and potential bugs
3. **SpotBugs**: Detects potential bugs through bytecode analysis
4. **Checkstyle**: Verifies coding standards compliance
5. **OWASP Dependency Check**: Identifies known security vulnerabilities in dependencies

### Options

The script supports several options:

```
Usage: ./scripts/quality-check.sh [options]
Options:
  --skip-format     Skip code formatting with Spotless
  --skip-spotbugs   Skip SpotBugs checks
  --skip-checkstyle Skip Checkstyle checks
  --skip-depcheck   Skip OWASP Dependency Check
  --fix             Fix issues when possible (currently only formatting)
  --help            Show this help message
```

### Fixing Issues

To automatically fix formatting issues:

```bash
./scripts/quality-check.sh --fix
```

### Customizing Rules

The quality checks are configured using XML files in the `src/main/resources` directory:

- `spotbugs-exclude.xml`: SpotBugs exclusions
- `checkstyle-rules.xml`: Checkstyle rule configuration
- `dependency-check-suppressions.xml`: OWASP dependency check suppressions
