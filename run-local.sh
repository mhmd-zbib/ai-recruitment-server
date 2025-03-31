#!/bin/bash

# Start only the dependencies in the background with Docker Compose
echo "Starting dependencies with Docker Compose..."
docker-compose up -d

# Set up environment variables from .env file
ENV_FILE="/.env"
if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE..."
    export $(grep -v '^#' $ENV_FILE | xargs)
    
    # Override with development-specific variables
    export JDBC_DATABASE_URL="$DEV_JDBC_DATABASE_URL"
    export JDBC_DATABASE_USERNAME="$DEV_JDBC_DATABASE_USERNAME" 
    export JDBC_DATABASE_PASSWORD="$DEV_JDBC_DATABASE_PASSWORD"
    export MINIO_HOST="$DEV_MINIO_HOST"
    export MINIO_PORT="$DEV_MINIO_PORT"
    export MINIO_ACCESS_KEY="$DEV_MINIO_ACCESS_KEY"
    export MINIO_SECRET_KEY="$DEV_MINIO_SECRET_KEY"
    export MINIO_BUCKET_NAME="$DEV_MINIO_BUCKET_NAME"
    export MINIO_ENABLED="$DEV_MINIO_ENABLED"
else
    echo "Warning: .env file not found at $ENV_FILE"
fi

# Set Spring profile to dev
export SPRING_PROFILES_ACTIVE=dev

# Run the application with Maven
echo "Starting application with profile $SPRING_PROFILES_ACTIVE..."
mvn spring-boot:run 