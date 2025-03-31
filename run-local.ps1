# PowerShell version of run-local.sh

# Start only the dependencies in the background with Docker Compose
Write-Host "Starting dependencies with Docker Compose..."
docker-compose up -d

# Set up environment variables from .env file
$ENV_FILE = "src/main/resources/.env"
if (Test-Path $ENV_FILE) {
    Write-Host "Loading environment variables from $ENV_FILE..."
    Get-Content $ENV_FILE | ForEach-Object {
        if (-not $_.StartsWith("#") -and $_.Length -gt 0) {
            $key, $value = $_.Split('=', 2)
            [Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
        }
    }
    
    # Override with development-specific variables
    $env:JDBC_DATABASE_URL = $env:DEV_JDBC_DATABASE_URL
    $env:JDBC_DATABASE_USERNAME = $env:DEV_JDBC_DATABASE_USERNAME
    $env:JDBC_DATABASE_PASSWORD = $env:DEV_JDBC_DATABASE_PASSWORD
    $env:MINIO_HOST = $env:DEV_MINIO_HOST
    $env:MINIO_PORT = $env:DEV_MINIO_PORT
    $env:MINIO_ACCESS_KEY = $env:DEV_MINIO_ACCESS_KEY
    $env:MINIO_SECRET_KEY = $env:DEV_MINIO_SECRET_KEY
    $env:MINIO_BUCKET_NAME = $env:DEV_MINIO_BUCKET_NAME
    $env:MINIO_ENABLED = $env:DEV_MINIO_ENABLED
} else {
    Write-Host "Warning: .env file not found at $ENV_FILE"
}

# Set Spring profile to dev
$env:SPRING_PROFILES_ACTIVE = "dev"

# Run the application with Maven
Write-Host "Starting application with profile $env:SPRING_PROFILES_ACTIVE..."
mvn spring-boot:run 