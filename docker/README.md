# Docker Configuration for HireSync

This directory contains Docker configuration files for the HireSync application.

## Files

- `Dockerfile`: Multi-stage Dockerfile for building and running the application
- `docker-compose.yaml`: Production Docker Compose configuration
- `docker-compose.test.yaml`: Testing Docker Compose configuration

## Usage

### Development Environment

To run the application in a development environment:

```bash
# From the project root directory
docker compose -f docker/docker-compose.yaml up -d
```

### Testing Environment

To run tests in a containerized environment:

```bash
# From the project root directory
docker compose -f docker/docker-compose.test.yaml build
docker compose -f docker/docker-compose.test.yaml up --abort-on-container-exit --exit-code-from app-test
```

### Production Environment

For production deployment, use environment variables to configure the application:

```bash
# Create a .env file with your production settings
cp .env.example .env
# Edit the .env file with your production values
nano .env

# Run the application with production settings
docker compose -f docker/docker-compose.yaml up -d
```

## Environment Variables

The Docker setup uses the following environment variables:

### Database Configuration
- `DB_HOST`: Database hostname (default: postgres)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: hiresync)
- `DB_USERNAME`: Database username (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)

### JPA Configuration
- `JPA_DDL_AUTO`: Hibernate DDL auto setting (default: update)
- `JPA_SHOW_SQL`: Whether to show SQL in logs (default: false)

### Application Configuration
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
- `SERVER_PORT`: Server port (default: 8080)
- `JAVA_OPTS`: JVM options (default: -Xms512m -Xmx1024m)

### Security Configuration
- `JWT_SECRET`: Secret key for JWT tokens
- `JWT_EXPIRATION`: JWT token expiration time in milliseconds (default: 3600000)
- `JWT_REFRESH_EXPIRATION`: JWT refresh token expiration time in milliseconds (default: 86400000)
- `JWT_ISSUER`: JWT issuer (default: hiresync)
- `JWT_AUDIENCE`: JWT audience (default: hiresync-users)

## Volumes

The Docker Compose setup uses the following named volumes:

- `hiresync_postgres_data`: Persistent storage for the database
- `hiresync_app_logs`: Persistent storage for application logs
- `hiresync_postgres_test_data`: Persistent storage for the test database
- `maven-repo`: Maven repository cache for faster builds (test only)

## Networks

- `hiresync_network`: Isolated network for application containers
- `hiresync_test_network`: Isolated network for test containers