# HireSync Scripts

This directory contains utility scripts for development, deployment, and maintenance of the HireSync application.

## Available Scripts

### Core Application Scripts

- **start.sh**: Starts all services and the application in production-like mode
- **start-local.sh**: Starts a local development environment with hot reloading
- **stop.sh**: Stops all services
- **restart.sh**: Restarts all services and the application
- **status.sh**: Shows the status of all services

### Utility Scripts

- **app.sh**: Starts only the Spring Boot application
- **services.sh**: Starts only the supporting services (PostgreSQL, etc.)
- **clean.sh**: Stops services and removes volumes (data reset)
- **help.sh**: Shows help information for all commands

### Quality & Testing Scripts

- **lint.sh**: Runs comprehensive code quality and linting checks
- **checkstyle.sh**: Runs code style checks

### Core Utilities

- **common.sh**: Core utilities and shared functions
- **app-utils.sh**: Application-related utility functions
- **docker-utils.sh**: Docker-related utility functions

## Usage Examples

### Local Development

For local development with hot reloading:

```bash
./start-local.sh
```

With auto migrations and development data:

```bash
AUTO_MIGRATE=true SEED_DEV_DATA=true ./start-local.sh
```

### Code Quality Checks

Run all linting checks:

```bash
./lint.sh
```

Run just checkstyle:

```bash
./checkstyle.sh
```

### Service Management

Start just the backing services:

```bash
./services.sh
```

Check service status:

```bash
./status.sh
```

Clean up and remove data:

```bash
./clean.sh
```

## Configuration

Most scripts rely on environment variables defined in the `.env` file at the project root. See the main README.md for details on available environment variables. 