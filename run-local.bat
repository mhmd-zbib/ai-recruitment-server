@echo off
SETLOCAL EnableDelayedExpansion

REM Start only the dependencies in the background with Docker Compose
echo Starting dependencies with Docker Compose...
docker-compose up -d

REM Set up environment variables from .env file
SET ENV_FILE=src\main\resources\.env
IF EXIST %ENV_FILE% (
    echo Loading environment variables from %ENV_FILE%...
    
    REM Process .env file and extract variables
    FOR /F "tokens=*" %%A IN ('type %ENV_FILE% ^| findstr /V "^#"') DO (
        SET line=%%A
        IF NOT "!line!"=="" (
            FOR /F "tokens=1,2 delims==" %%B IN ("!line!") DO (
                SET key=%%B
                SET value=%%C
                SET !key!=!value!
            )
        )
    )
    
    REM Override with development-specific variables
    SET JDBC_DATABASE_URL=!DEV_JDBC_DATABASE_URL!
    SET JDBC_DATABASE_USERNAME=!DEV_JDBC_DATABASE_USERNAME!
    SET JDBC_DATABASE_PASSWORD=!DEV_JDBC_DATABASE_PASSWORD!
    SET MINIO_HOST=!DEV_MINIO_HOST!
    SET MINIO_PORT=!DEV_MINIO_PORT!
    SET MINIO_ACCESS_KEY=!DEV_MINIO_ACCESS_KEY!
    SET MINIO_SECRET_KEY=!DEV_MINIO_SECRET_KEY!
    SET MINIO_BUCKET_NAME=!DEV_MINIO_BUCKET_NAME!
    SET MINIO_ENABLED=!DEV_MINIO_ENABLED!
) ELSE (
    echo Warning: .env file not found at %ENV_FILE%
)

REM Set Spring profile to dev
SET SPRING_PROFILES_ACTIVE=dev

REM Run the application with Maven
echo Starting application with profile %SPRING_PROFILES_ACTIVE%...
mvn spring-boot:run

ENDLOCAL 