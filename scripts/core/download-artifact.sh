#!/usr/bin/env bash

# Description: Downloads build artifacts from a repository or registry

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/../utils/logging.sh"

# Default values
ARTIFACT_TYPE="jar"    # jar, docker
REGISTRY="ghcr.io"     # For Docker images
ORG="zbib"             # For Docker images
IMAGE_NAME="hiresync"  # For Docker images
TAG="latest"           # For Docker images
VERSION=""             # For JAR files
REPOSITORY_URL=""      # For JAR files
OUTPUT_DIR="$PROJECT_ROOT/target"
OVERWRITE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --type=*)
      ARTIFACT_TYPE="${1#*=}"
      shift
      ;;
    --registry=*)
      REGISTRY="${1#*=}"
      shift
      ;;
    --org=*)
      ORG="${1#*=}"
      shift
      ;;
    --image=*)
      IMAGE_NAME="${1#*=}"
      shift
      ;;
    --tag=*)
      TAG="${1#*=}"
      shift
      ;;
    --version=*)
      VERSION="${1#*=}"
      shift
      ;;
    --repo=*)
      REPOSITORY_URL="${1#*=}"
      shift
      ;;
    --output=*)
      OUTPUT_DIR="${1#*=}"
      shift
      ;;
    --overwrite)
      OVERWRITE=true
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      echo "Usage: ./hiresync download-artifact [--type=jar|docker] [--registry=REGISTRY] [--org=ORG] [--image=IMAGE] [--tag=TAG] [--version=VERSION] [--repo=URL] [--output=DIR] [--overwrite]"
      exit 1
      ;;
  esac
done

# Check for GitHub-specific environment variables
if [ -n "$GITHUB_REPOSITORY_OWNER" ] && [ "$ORG" == "zbib" ]; then
  ORG="$GITHUB_REPOSITORY_OWNER"
  log_info "Using GitHub repository owner: $ORG"
fi

# Create output directory if it doesn't exist
if [ ! -d "$OUTPUT_DIR" ]; then
  log_info "Creating output directory: $OUTPUT_DIR"
  mkdir -p "$OUTPUT_DIR"
fi

log_section "Downloading Artifact"

# Download based on artifact type
case "$ARTIFACT_TYPE" in
  jar)
    if [ -z "$VERSION" ]; then
      log_error "Version is required for JAR downloads"
      echo "Please provide a version with --version=VERSION"
      exit 1
    fi
    
    if [ -z "$REPOSITORY_URL" ]; then
      log_error "Repository URL is required for JAR downloads"
      echo "Please provide a repository URL with --repo=URL"
      exit 1
    fi
    
    JAR_NAME="hiresync-$VERSION.jar"
    OUTPUT_FILE="$OUTPUT_DIR/$JAR_NAME"
    
    if [ -f "$OUTPUT_FILE" ] && [ "$OVERWRITE" != "true" ]; then
      log_warning "Artifact already exists: $OUTPUT_FILE"
      log_info "Use --overwrite to replace it"
      exit 0
    fi
    
    log_info "Downloading JAR from repository"
    curl -L "$REPOSITORY_URL/$JAR_NAME" -o "$OUTPUT_FILE"
    
    if [ $? -ne 0 ]; then
      log_error "Failed to download JAR: $JAR_NAME"
      exit 1
    else
      log_success "Successfully downloaded JAR to: $OUTPUT_FILE"
    fi
    ;;
    
  docker)
    FULL_IMAGE_NAME="$REGISTRY/$ORG/$IMAGE_NAME:$TAG"
    log_info "Pulling Docker image: $FULL_IMAGE_NAME"
    
    docker pull "$FULL_IMAGE_NAME"
    
    if [ $? -ne 0 ]; then
      log_error "Failed to pull Docker image: $FULL_IMAGE_NAME"
      exit 1
    else
      log_success "Successfully pulled Docker image: $FULL_IMAGE_NAME"
    fi
    ;;
    
  *)
    log_error "Unknown artifact type: $ARTIFACT_TYPE"
    echo "Valid types: jar, docker"
    exit 1
    ;;
esac 