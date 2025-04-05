# Docker Configuration for HireSync

This directory contains Docker and container configurations for the HireSync application.

## Directory Structure

- `docker-compose.local.yaml`: Local development environment configuration
- `docker-compose.prod.yaml`: Production environment configuration
- `Dockerfile`: Application container build configuration

## Automated PostgreSQL Setup

The PostgreSQL container is configured to automatically set up the database environment based on variables defined in your `.env` file.

### How it works

PostgreSQL's official Docker image provides built-in automation:

1. When the container starts, it automatically:
   - Creates a PostgreSQL user with the credentials specified in `POSTGRES_USER` and `POSTGRES_PASSWORD`
   - Creates a database with the name specified in `POSTGRES_DB`
   - Sets the created user as the owner of the database

2. Our `docker-compose.local.yaml` file maps these directly to your `.env` variables:
   - `POSTGRES_USER: ${DB_USER:-hiresync}`
   - `POSTGRES_PASSWORD: ${DB_PASSWORD:-hiresync}`
   - `POSTGRES_DB: ${DB_NAME:-hiresync}`

### Environment Variables

The PostgreSQL initialization uses these variables from your `.env` file:

- `DB_NAME`: The name of the database to create (default: hiresync)
- `DB_USER`: The username to create (default: hiresync)
- `DB_PASSWORD`: The password for the user (default: hiresync)
- `DB_PORT`: The port mapping on the host (default: 5433)

### Container Access

- From host machine: `localhost:5433` (or your configured `DB_PORT`)
- From other containers: `postgres:5432` (internal network)

## Network Configuration

All containers are connected to a custom bridge network (`hiresync-network` by default) to enable container-to-container communication. 