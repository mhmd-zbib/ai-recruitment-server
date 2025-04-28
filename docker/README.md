# Docker Configuration for HireSync

This directory contains Docker configuration files for the HireSync application.

## Files

- `Dockerfile`: Main Dockerfile for production builds
- `Dockerfile.test`: Specialized Dockerfile for testing
- `docker-compose.test.yaml`: Docker Compose configuration for running tests

## Usage

### Running Tests

To run tests in a containerized environment:

```bash
docker compose -f docker-compose.test.yaml build
docker compose -f docker-compose.test.yaml up --abort-on-container-exit --exit-code-from app-test
```

### Building Production Image

To build the production Docker image:

```bash
docker build -t hiresync:latest -f Dockerfile ..
```

## Configuration

The Docker setup uses the following environment variables:

### Database Configuration
- `DB_HOST`: Database hostname (default: postgres_db)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

### Application Configuration
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
- `JAVA_OPTS`: JVM options (default: -Xms512m -Xmx1024m)
- `JWT_SECRET`: Secret key for JWT tokens
- `JWT_ISSUER`: JWT issuer
- `JWT_AUDIENCE`: JWT audience

## Volumes

The Docker Compose setup uses the following named volumes:

- `hiresync_postgres_test_data`: Persistent storage for the test database
- `hiresync_maven_repo`: Maven repository cache for faster builds

## Networks

- `hiresync_test_network`: Isolated network for test containers
