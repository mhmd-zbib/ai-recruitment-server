# HireSync Scripts

This directory contains the script files that power the HireSync development environment manager.

## Script Structure

### Core Utilities

- `common.sh`: Core functions for logging, error handling, and environment management
- `docker-utils.sh`: Docker infrastructure and service management
- `app-utils.sh`: Spring Boot application management

### Command Scripts

- `start.sh`: Starts all services and the application
- `stop.sh`: Stops all services
- `restart.sh`: Restarts all services and the application
- `status.sh`: Shows status of services
- `app.sh`: Starts only the Spring Boot application
- `services.sh`: Starts only the supporting services (PostgreSQL, etc.)
- `clean.sh`: Stops services and removes volumes (data reset)
- `help.sh`: Displays usage information

### Main Launcher

The main script `hiresync` in the project root orchestrates these scripts, providing a unified interface for managing the development environment.

## Using the Main Launcher

All operations are handled by the main launcher script:

```bash
# Start everything (PostgreSQL + application)
./hiresync start

# Start only services (PostgreSQL, etc.)
./hiresync services

# Stop everything
./hiresync stop

# Show status of all services
./hiresync status

# Start only the Spring Boot application
./hiresync app

# Restart all services and application
./hiresync restart

# Clean up environment
./hiresync clean
```

## Architecture

The scripts follow a modular design:

1. **Core utilities** (`common.sh`): Base functions for logging, error handling, and environment loading
2. **Domain utilities**:
   - Docker service management (`docker-utils.sh`)
   - Application management (`app-utils.sh`)
3. **Command scripts**: Each command is implemented in its own script which uses the utility modules

This modular structure makes maintenance and extension easier, and separates concerns for better organization.

## Environment Configuration

The scripts automatically load configuration from the `.env` file in the project root. If this file doesn't exist, it will be created from `.env.example` if available. 