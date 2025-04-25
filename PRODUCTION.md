# HireSync Production Deployment Guide

This guide explains how to deploy HireSync to production.

## Prerequisites

- Docker and Docker Compose
- Access to your Docker Hub account
- A server with at least 2GB RAM, 1 CPU, 20GB disk

## Deployment Steps

1. **Clone the repository**

```bash
git clone https://github.com/your-repo/hiresync.git
cd hiresync
```

2. **Set up production environment**

Copy the production environment template:

```bash
cp .env.prod.template .env
```

Edit the `.env` file with your production values:
- Set database credentials
- Set a secure JWT secret
- Set your domain for CORS

3. **Deploy the application**

Run the deployment script:

```bash
./scripts/deploy-prod.sh
```

4. **Verify deployment**

Check if the application is running:

```bash
docker ps
curl http://localhost:8080/api/actuator/health
```

5. **Monitor the application**

Run the monitoring script:

```bash
./scripts/monitor-prod.sh
```

## Maintenance

### Updating the application

1. Pull the latest code:
```bash
git pull origin master
```

2. Re-deploy:
```bash
./scripts/deploy-prod.sh
```

### Viewing logs

```bash
docker logs hiresync-app
```

### Backing up the database

```bash
docker exec -t postgres pg_dump -U hiresync -d hiresync > backup.sql
```

## Troubleshooting

- If the application fails to start, check logs:
```bash
docker logs hiresync-app
```

- If you can't connect to the database:
```bash
docker logs postgres
```

- If you need to restart:
```bash
docker-compose -f docker/docker-compose.prod.yaml restart
``` 