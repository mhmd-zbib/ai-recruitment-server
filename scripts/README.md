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
│   └── dev-environment.sh     # Unified development environment manager
├── quality/       # Quality check scripts
│   ├── lint-minimal.sh        # Run minimal linting with auto-fixes
│   └── quality-check.sh       # Run comprehensive quality checks with auto-fixes
└── utils/         # Utility functions
    ├── db-utils.sh            # Database utility functions
    └── health-check.sh        # Application health monitoring
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

# Auto-fix code issues
./run.sh lint

# Run quality checks (non-blocking)
./run.sh quality

# Check application health status
./run.sh health
```

## Auto-Fix Linting

The project includes automated linting and formatting capabilities that will automatically fix common issues:

- Code formatting is auto-fixed using Spotless
- Line endings are normalized
- Import organization is attempted
- Code structure issues are identified (but require manual fixes)

To run auto-fixes:

```bash
./run.sh lint
```

These auto-fixes are also applied automatically during git commit through pre-commit hooks.

## Script Guidelines

1. All scripts must be executable and have proper shebang line: `#!/bin/bash`
2. Scripts should use the shared color definitions for consistent output
3. All scripts should perform proper error checking but prefer auto-fixes over failing
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
1. Auto-fix issues when possible
2. Continue despite errors unless absolutely impossible
3. Provide clear guidance on fixing remaining issues
4. Clean up resources on failure
5. Log errors to appropriate output

## Best Practices

1. Run the lint command before committing changes
2. Use appropriate environment variables
3. Follow the deployment checklist
4. Monitor logs for issues
5. Keep scripts up to date with changes 