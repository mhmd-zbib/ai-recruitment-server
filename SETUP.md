# HireSync Local Development Setup Guide

This guide provides step-by-step instructions to set up the HireSync development environment on your local machine.

## Prerequisites

Ensure you have the following installed:

- JDK 17 or later
- Docker and Docker Compose
- Git
- Bash shell (included in Git Bash for Windows)

## Initial Setup

1. **Clone the repository**

```bash
git clone https://github.com/zbib/hiresync.git
cd hiresync
```

2. **Make the hiresync script executable**

```bash
# Unix/macOS/Git Bash
chmod +x hiresync
```

3. **Initialize the development environment**

```bash
./hiresync setup
```

This will:
- Create a `.env` file from `.env.example` if it doesn't exist
- Make all necessary scripts executable

## Using the Development Environment

HireSync includes a unified script that makes it easy to manage your development environment.

```bash
./hiresync help
```

### Available Commands

| Command | Description |
|---------|-------------|
| `./hiresync start` | Start local development environment (PostgreSQL + Spring Boot) |
| `./hiresync stop` | Stop local development environment |
| `./hiresync restart` | Restart local development environment |
| `./hiresync status` | Check status of local development environment |
| `./hiresync check` | Verify environment configuration |
| `./hiresync logs` | View PostgreSQL container logs |
| `./hiresync psql` | Open PostgreSQL interactive terminal |
| `./hiresync setup` | Initial environment setup (create .env, make scripts executable) |
| `./hiresync clean` | Remove all data and start fresh (WARNING: destroys data) |
| `./hiresync help` | Show help message |

### Common Workflows

#### Starting the Environment

```bash
./hiresync start
```

This command:
1. Loads environment variables from `.env`
2. Checks if Docker is running
3. Starts PostgreSQL in a Docker container
4. Waits for PostgreSQL to be ready
5. Starts the Spring Boot application with the local profile

The application will be available at:
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Documentation: http://localhost:8080/v3/api-docs

#### Checking Environment Status

To verify your environment setup and the status of the services:

```bash
./hiresync status
```

This will check:
- If Docker is running
- If the PostgreSQL container is running and ready
- If the required ports are accessible

#### Stopping the Environment

When you're done working, stop the environment:

```bash
./hiresync stop
```

This will stop and remove the PostgreSQL Docker container, but will preserve the database volume.

#### Interactive Database Access

To access the PostgreSQL database directly:

```bash
./hiresync psql
```

This opens an interactive PostgreSQL terminal.

#### Viewing Database Logs

To view the PostgreSQL container logs:

```bash
./hiresync logs
```

#### Complete Reset

If you need to start fresh and remove all data:

```bash
./hiresync clean
```

**WARNING**: This will delete all database data!

## Troubleshooting

### Docker Issues

- Ensure Docker is running before starting the environment
- If you encounter port conflicts, check the `DB_PORT` value in your `.env` file
- To reset the database completely, run: `./hiresync clean`

### Spring Boot Issues

- Check the Spring Boot logs for error messages
- Verify the database connection settings in `application-local.yaml`
- Make sure all required environment variables are set in `.env`
- Run `./hiresync check` to verify your configuration

### Windows-Specific Issues

- Use Git Bash to run the scripts with Unix-like commands: `./hiresync <command>`
- Windows paths may differ; ensure the script can access Docker and Docker Compose
- If you get "command not found" errors, ensure bash is installed and in your PATH
- Run Git Bash as Administrator if you encounter permission issues

## Database Management

The PostgreSQL database is configured with:

- A main database named `hiresync_db` (or as specified in `DB_NAME`)
- A `hiresync` schema
- The following roles:
  - Main user with username from `DB_USERNAME` and password from `DB_PASSWORD`
  - Read-only user (`readonly_user`) with password from `READ_PASSWORD`
  - Read-write user (`readwrite_user`) with password from `WRITE_PASSWORD`

## Custom Configuration

To customize the PostgreSQL configuration, edit the command section in `docker/docker-compose.local.yaml`.

For Spring Boot customization, modify the appropriate settings in `src/main/resources/application-local.yaml`.

## Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/) 