# HireSync Development Guide

This guide describes how to set up and run the HireSync application for development.

## Prerequisites

- Docker and Docker Compose
- Git
- A terminal

## Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/your-repo/hiresync.git
   cd hiresync
   ```

2. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

3. Start the development environment:
   ```bash
   ./scripts/dev.sh
   ```

The application will be available at http://localhost:8080/api and Swagger UI at http://localhost:8080/api/swagger-ui.html

## Development Scripts

We provide simple scripts to make development easier:

- `./scripts/dev.sh` - Start the application in development mode
  - Use `--debug` flag for more verbose logging
  - Use `--restart` flag to restart containers if they're already running

## Configuration

The main configuration files are:

- `.env` - Environment variables for local development
- `src/main/resources/application-local.yaml` - Spring Boot configuration for local development
- `docker/docker-compose.local.yaml` - Docker Compose configuration

## Environment Variables

Important environment variables you can set in your `.env` file:

| Variable | Default | Description |
|----------|---------|-------------|
| PORT | 8080 | Port where the application will be available |
| DB_PORT | 5432 | PostgreSQL port on host machine |
| DB_NAME | hiresync | Database name |
| DB_USER | hiresync | Database username |
| DB_PASSWORD | hiresync | Database password |
| JWT_SECRET | (generated) | JWT secret key for authentication |
| DEBUG_MODE | false | Enable verbose logging |

## Docker Container Structure

The development environment consists of two containers:

1. **hiresync-postgres** - PostgreSQL database
   - Internal port: 5432
   - External port: Controlled by DB_PORT in .env (default: 5432)

2. **hiresync-devtools** - Development container with Maven and JDK
   - Application port: 8080 inside container, mapped to PORT in .env on host
   - Debug port: 5005 (for remote debugging)

## Debugging

Remote debugging is available on port 5005. Configure your IDE to connect to this port.

### IntelliJ IDEA

1. Go to Run → Edit Configurations
2. Add new Remote JVM Debug configuration
3. Set host to `localhost` and port to `5005`
4. Set module classpath as needed
5. Save and use this configuration to start debugging

### VS Code

Add this to your `.vscode/launch.json`:

```json
{
  "configurations": [
    {
      "type": "java",
      "name": "Attach to HireSync",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

## Logs and Monitoring

- Application logs are displayed in the terminal when running `./scripts/dev.sh`
- To view container logs separately:
  ```bash
  docker logs hiresync-devtools -f
  docker logs hiresync-postgres -f
  ```

## Common Issues

### Port conflicts

If you see an error like "Port 8080 is already in use":

1. Edit your `.env` file and change the PORT variable
2. Restart with `./scripts/dev.sh --restart`

### Database connection issues

Ensure PostgreSQL is running:

```bash
docker ps | grep postgres
```

If not running, restart containers with `./scripts/dev.sh --restart`

If you're still having connection issues, check the connection details:

```bash
docker exec -it hiresync-postgres psql -U hiresync -c "\l"
```

### Container won't start

Check Docker logs for details:

```bash
docker logs hiresync-devtools
```

### Application fails to start

Common issues include:
- Database connection problems
- Port conflicts
- Java compilation errors

Check the logs displayed by `./scripts/dev.sh` for specific error messages. 