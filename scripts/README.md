# HireSync Scripts

This directory contains utility scripts for managing the HireSync application and its supporting services.

## Directory Structure

```
scripts/
├── core/                 # Core application scripts
│   ├── app.sh            # Application runner
│   ├── local.sh          # Local development with hot reload
│   ├── services.sh       # Service management
│   └── start.sh          # Production starter
├── utils/                # Utility scripts and functions
│   ├── app.sh            # Application management utilities
│   ├── docker.sh         # Docker and service utilities
│   ├── logging.sh        # Logging and output formatting
│   └── lint.sh           # Code quality checks
└── README.md             # This file
```

## Common Scripts

### core/start.sh

Start the application in production mode with all required services.

```
./core/start.sh
```

### core/local.sh

Run the application in local development mode with hot reload.

```
./core/local.sh [OPTIONS]

Options:
  --no-services  Don't start Docker services
  --debug        Enable remote debugging
  --seed         Seed development data
  --migrate      Run database migrations
  --help, -h     Show help
```

### core/app.sh

Run the application without starting services.

```
./core/app.sh [PROFILE] [OPTIONS]

Profiles:
  local    Development mode with hot reload (default)
  dev      Development mode with remote database
  test     Testing environment
  prod     Production mode

Options:
  --debug  Enable remote debugging
```

### core/services.sh

Manage Docker services without starting the application.

```
./core/services.sh COMMAND [OPTIONS]

Commands:
  start     Start all services
  stop      Stop all services
  restart   Restart all services
  status    Show services status
  logs      Show service logs
  clean     Stop and remove volumes

Options:
  --help, -h  Show help
```

## Utility Functions

The scripts under the `utils/` directory provide shared functionality:

- **app.sh**: Functions for running, packaging, and testing the Spring Boot application
- **docker.sh**: Functions for managing Docker containers and services
- **logging.sh**: Functions for formatted output and user interaction
- **lint.sh**: Code quality checking utilities

## Examples

1. Start in local development mode with database migrations:
   ```
   ./core/local.sh --migrate
   ```

2. Start only the services without the application:
   ```
   ./core/services.sh start
   ```

3. Start the application in production mode:
   ```
   ./core/start.sh
   ```

4. Run the application with a specific profile:
   ```
   ./core/app.sh prod
   ```

5. Run the application with debugging enabled:
   ```
   ./core/app.sh local --debug
   ```

## Implementation Status

✅ Complete
- utils/logging.sh - Logging utilities
- utils/docker.sh - Docker management utilities
- utils/app.sh - Application utilities
- utils/lint.sh - Code quality checks
- core/app.sh - Application runner
- core/local.sh - Local development script
- core/services.sh - Service management
- core/start.sh - Production starter
- README.md - Documentation

## Recent Changes

This scripts directory has been completely reorganized with the following improvements:

1. **Simpler Structure**: Reduced to just core/ and utils/ directories
2. **Simplified Commands**: Clearer interfaces with consistent parameter handling
3. **Better Error Handling**: More robust error checking and user feedback
4. **Comprehensive Help**: All scripts provide help text and examples
5. **Consistent Logging**: Unified logging system with clear visual formatting 