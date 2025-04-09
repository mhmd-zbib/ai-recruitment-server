# HireSync Testing Scripts

## Test Script

The `test.sh` script provides a unified way to run tests across different environments:

- Local machine
- Dev container
- Docker Compose environment
- CI pipeline

### Usage

```bash
./scripts/core/test.sh [OPTIONS]
```

### Options

- `--type=TYPE`: Test type (unit|integration|e2e|all), default: all
- `--env=ENV`: Environment (local|container|docker), default: auto-detect
- `--profile=PROFILE`: Spring profile to use, default: test
- `--build-image`: Build Docker image before running tests (docker env only)
- `--keep-containers`: Don't remove containers after tests (docker env only)
- `--no-reports`: Skip generating test reports
- `--maven-opts=OPTS`: Additional Maven options
- `--args=ARGS`: Additional arguments for test command
- `--help`: Show help message

### Examples

Run unit tests using auto-detected environment:
```bash
./scripts/core/test.sh --type=unit
```

Run all tests in Docker with a fresh image:
```bash
./scripts/core/test.sh --env=docker --build-image
```

Run integration tests in the dev container:
```bash
./scripts/core/test.sh --type=integration --env=container
```

### Environment Auto-Detection

If no environment is specified, the script will detect the appropriate environment:

1. If running in CI: Uses 'local' mode
2. If dev container is running: Uses 'container' mode
3. If docker-compose is available: Uses 'docker' mode
4. Otherwise: Uses 'local' mode

### Backwards Compatibility

The script supports legacy parameters from the previous test scripts:

- `--unit`: Equivalent to `--type=unit`
- `--integration`: Equivalent to `--type=integration`
- `--container`: Equivalent to `--env=container`

This makes it a drop-in replacement for the previous testing scripts. 