# HireSync Scripts

This directory contains utility scripts for development, deployment, and maintenance of the HireSync application.

## Directory Structure

- **core/**: Core utilities used by other scripts
  - `colors.sh`: Terminal color definitions
  - `logging.sh`: Logging functions with different levels
  - `env.sh`: Environment variable handling

- **db/**: Database management scripts
  - `db.sh`: Functions for database operations

- **utils/**: Utility scripts
  - `docker.sh`: Docker management utilities

- **ci/**: Continuous Integration scripts
  - *Contains CI-related scripts*

- **dev/**: Development environment scripts
  - *Contains development environment scripts*

## Core Utilities

### Logging

```bash
# Import logging utilities
source ./scripts/core/logging.sh

# Use logging functions
log_info "Information message"
log_error "Error message"
log_warn "Warning message"
log_debug "Debug message"
log_success "Success message"
```

### Color Output

```bash
# Import color definitions
source ./scripts/core/colors.sh

# Use colors in echo statements
echo -e "${BOLD_GREEN}Success${NC}"
echo -e "${BOLD_RED}Error${NC}"
```

## Database Management

The `db.sh` script provides utilities for managing the PostgreSQL database:

```bash
# Import database functions
source ./scripts/db/db.sh

# Start the database
db_start

# Check database status
db_status

# Initialize database schema
db_init

# Create a backup
db_backup ./backups

# Restore from backup
db_restore ./backups/hiresync_db_20240601_120000.sql

# Stop the database
db_stop
```

## Docker Utilities

The `docker.sh` script provides Docker management utilities:

```bash
# Import Docker utilities
source ./scripts/utils/docker.sh

# Build Docker image
docker_build latest

# Start Docker Compose stack
docker_up ./.env ./docker/docker-compose.yaml

# View logs
docker_logs app

# Stop Docker Compose stack
docker_down

# Clean up Docker resources
docker_clean
```

## Usage in Applications

These scripts are typically used by the main `run` script at the root of the project, but they can also be sourced and used in other scripts or directly from the command line.

### Direct Usage Example

```bash
# Starting the database
bash ./scripts/db/db.sh
db_start

# Building a Docker image
bash ./scripts/utils/docker.sh
docker_build latest
```

## Best Practices

When adding new scripts:

1. Use the same style and conventions as existing scripts
2. Add proper logging and error handling
3. Make functions reusable and modular
4. Add proper documentation in this README
5. Export functions you want to make available to other scripts
6. Use constants instead of hardcoded values
7. Check for required dependencies
8. Use meaningful variable and function names
9. Add help text and usage examples 