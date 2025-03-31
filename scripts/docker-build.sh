#!/bin/bash

# Script to build Docker images for HireSync application

# Get script directory for reliable sourcing
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
VERSION="latest"
BUILD_FAST=false
PUSH=false
REGISTRY=""

# Print header
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}HireSync Docker Image Builder${NC}"
echo -e "${BLUE}========================================${NC}"

# Process command-line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --version=*)
      VERSION="${1#*=}"
      shift
      ;;
    --fast)
      BUILD_FAST=true
      shift
      ;;
    --push)
      PUSH=true
      shift
      ;;
    --registry=*)
      REGISTRY="${1#*=}"
      shift
      ;;
    --help)
      echo -e "Usage: ./scripts/docker-build.sh [options]"
      echo -e "Options:"
      echo -e "  --version=VERSION     Set the image version tag (default: latest)"
      echo -e "  --fast                Use the fast build Dockerfile (skips tests)"
      echo -e "  --push                Push the image to the registry after building"
      echo -e "  --registry=REGISTRY   Set the Docker registry (e.g., docker.io/yourusername)"
      echo -e "  --help                Display this help message"
      echo -e ""
      echo -e "Examples:"
      echo -e "  ./scripts/docker-build.sh --version=1.0.0"
      echo -e "  ./scripts/docker-build.sh --fast --push --registry=docker.io/hiresync"
      exit 0
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Check Docker availability
if ! docker info > /dev/null 2>&1; then
  echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
  exit 1
fi

# Set image name based on registry
if [ -n "$REGISTRY" ]; then
  IMAGE_NAME="${REGISTRY}/hiresync:${VERSION}"
else
  IMAGE_NAME="hiresync:${VERSION}"
fi

# Build the application JAR if using fast build
if [ "$BUILD_FAST" = true ]; then
  echo -e "${YELLOW}Building application JAR...${NC}"
  ./mvnw clean package -DskipTests
  
  # Build Docker image with Dockerfile.fast
  echo -e "${YELLOW}Building Docker image using Dockerfile.fast...${NC}"
  docker build -t "$IMAGE_NAME" -f Dockerfile.fast --build-arg VERSION="$VERSION" .
else
  # Build Docker image with standard Dockerfile
  echo -e "${YELLOW}Building Docker image using Dockerfile...${NC}"
  docker build -t "$IMAGE_NAME" -f Dockerfile --build-arg VERSION="$VERSION" .
fi

# Check if build was successful
if [ $? -ne 0 ]; then
  echo -e "${RED}Error: Docker build failed.${NC}"
  exit 1
fi

echo -e "${GREEN}Docker image built successfully: ${IMAGE_NAME}${NC}"

# Push the image if requested
if [ "$PUSH" = true ]; then
  echo -e "${YELLOW}Pushing Docker image to registry...${NC}"
  docker push "$IMAGE_NAME"
  
  if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Failed to push Docker image. Make sure you are logged in to the registry.${NC}"
    echo -e "${YELLOW}Use 'docker login' to authenticate.${NC}"
    exit 1
  fi
  
  echo -e "${GREEN}Docker image pushed successfully: ${IMAGE_NAME}${NC}"
fi

exit 0 