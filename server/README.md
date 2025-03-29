# HireSync API

HireSync is an AI-enhanced recruitment pipeline backend application built with Spring Boot.

## Features

- User authentication and authorization with JWT
- Job management
- Application tracking
- Interview scheduling
- Comprehensive logging
- Error handling

## Prerequisites

- Java 17
- Maven
- PostgreSQL
- Docker (optional)

## Local Development

### Option 1: Without Docker

1. Configure PostgreSQL and update `application.yaml` with your database credentials
2. Run the application:
```
mvn spring-boot:run
```

### Option 2: With Docker Compose

1. Build and run the application with Docker Compose:
```
docker-compose up -d
```

## Deploying to Render

### Method 1: Deploy from GitHub

1. Push your code to a GitHub repository
2. Create an account on [Render](https://render.com/)
3. Click **New** and select **Web Service**
4. Choose **Build and deploy from a Git repository**
5. Connect your GitHub account and select your repository
6. Configure the service:
   - **Name**: hiresync-api
   - **Region**: Choose the closest to your users
   - **Branch**: main (or your deployment branch)
   - **Runtime**: Docker
   - **Plan**: Free tier (or select a paid plan for production)
7. Set environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `JDBC_DATABASE_URL=postgres://...` (Get this from your Render PostgreSQL instance)
   - `JDBC_DATABASE_USERNAME=...`
   - `JDBC_DATABASE_PASSWORD=...`
   - `JWT_SECRET=...` (Generate a secure random string)
8. Click **Create Web Service**

### Method 2: Deploy from Docker Hub

1. Build your Docker image:
```
docker build -t yourusername/hiresync:latest .
```

2. Push to Docker Hub:
```
docker login
docker push yourusername/hiresync:latest
```

3. On Render:
   - Go to your profile settings
   - Enable **Early Access -> Deploy from external registries**
   - Create a new Web Service
   - Select **Deploy an existing image from a registry**
   - Enter your Docker Hub image URL
   - Configure environment variables as above
   - Deploy the service

## Setting Up a Database on Render

1. On Render dashboard, click **New** and select **PostgreSQL**
2. Configure your database:
   - **Name**: hiresync-db
   - **Database**: hiresync_db
   - **User**: hiresync_user
   - Select your preferred region and plan
3. After creation, use the Internal Database URL in your web service environment variables

## API Documentation

Once deployed, API documentation is available at:
- Locally: `http://localhost:8080/api/swagger-ui.html`
- On Render: `https://your-service-name.onrender.com/api/swagger-ui.html` 