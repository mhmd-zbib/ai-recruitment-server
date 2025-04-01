# HireSync Scripts

This directory contains all the shell scripts for the HireSync application. The scripts are organized by purpose to make them easier to find and maintain.

## Directory Structure

```
scripts/
├── build/         # Build-related scripts
│   ├── docker-build.sh        # Build Docker images
│   └── verify.sh              # Verify code quality before building
├── deploy/        # Deployment scripts
│   └── prod-deploy.sh         # Deploy to production
├── dev/           # Development scripts
│   ├── dev-start.sh           # Start in development mode
│   ├── local-dev.sh           # Simplified local development script
│   └── local-start.sh         # Start in local mode with Docker
├── quality/       # Quality check scripts
│   ├── lint-minimal.sh        # Run minimal linting checks
│   └── quality-check.sh       # Run comprehensive quality checks
└── utils/         # Utility functions
    └── db-utils.sh            # Database utility functions
```

## Usage

All scripts should be invoked through the main `run.sh` script in the project root:

```bash
# Run with bash
./run.sh [command] [options]
```

For example:

```bash
# Start local development
./run.sh local

# Build Docker image
./run.sh build

# Run quality checks
./run.sh quality
```

## Script Guidelines

1. All scripts must be executable and have proper shebang line: `#!/bin/bash`
2. Scripts should use the shared color definitions for consistent output
3. All scripts should perform proper error checking and exit with appropriate status codes
4. Scripts should be self-contained but leverage shared utilities when appropriate
5. Docker-related operations must use the DOCKER_DIR environment variable

## Environment Variables

Scripts can depend on these environment variables:

- `PROJECT_ROOT`: Root directory of the project
- `SCRIPT_DIR`: Directory containing the current script
- `DOCKER_DIR`: Directory containing Docker files

These environment variables can be set via the `.env` file at the project root.

## Common Options

Most scripts support these common options:
- `--help`: Show help message
- `--verbose`: Enable verbose output
- `--debug`: Enable debug mode
- `--dry-run`: Show what would be done without making changes

## Error Handling

All scripts follow these error handling principles:
1. Exit with non-zero status on error
2. Provide clear error messages
3. Clean up resources on failure
4. Log errors to appropriate output

## Best Practices

1. Always run quality checks before deployment
2. Use appropriate environment variables
3. Follow the deployment checklist
4. Monitor logs for issues
5. Keep scripts up to date with changes 