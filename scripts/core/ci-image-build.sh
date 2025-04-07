#!/bin/bash
set -e

# ===============================================================================
# ci-image-build.sh
#
# This script builds a Docker image for the HireSync application and optionally
# pushes it to the container registry.
#
# Usage:
#   ./scripts/core/ci-image-build.sh [--push]
#
# Options:
#   --push        Push the image to the container registry after building
# ===============================================================================

# Colorize terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default variables
PUSH_IMAGE=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKERFILE="${PROJECT_ROOT}/Dockerfile"
IMAGE_TAG="latest"

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --push)
      PUSH_IMAGE=true
      shift
      ;;
    *)
      echo -e "${RED}Unknown option: $1${NC}"
      exit 1
      ;;
  esac
done

# Check if we're in a CI environment and set variables accordingly
if [[ -n "${CI}" ]]; then
  # If in GitHub Actions
  if [[ -n "${GITHUB_ACTIONS}" ]]; then
    REGISTRY="${REGISTRY:-ghcr.io}"
    IMAGE_NAME="${IMAGE_NAME:-${GITHUB_REPOSITORY##*/}}"
    
    # Use branch name or tag for the image tag
    if [[ -n "${GITHUB_REF_NAME}" ]]; then
      if [[ "${GITHUB_REF_TYPE}" == "branch" ]]; then
        IMAGE_TAG="${GITHUB_REF_NAME}"
        # For master/main branch, also tag as latest
        if [[ "${GITHUB_REF_NAME}" == "master" || "${GITHUB_REF_NAME}" == "main" ]]; then
          ADDITIONAL_TAG="latest"
        fi
      else
        # For tags, use the tag name
        IMAGE_TAG="${GITHUB_REF_NAME}"
      fi
    fi
    
    # For pull requests, use PR number as tag
    if [[ "${GITHUB_EVENT_NAME}" == "pull_request" ]]; then
      PR_NUMBER=$(echo "${GITHUB_REF}" | grep -oE '[0-9]+')
      IMAGE_TAG="pr-${PR_NUMBER}"
    fi
  fi
fi

# Ensure we have the required variables
if [[ -z "${REGISTRY}" || -z "${IMAGE_NAME}" ]]; then
  echo -e "${RED}Error: REGISTRY and IMAGE_NAME must be set${NC}"
  exit 1
fi

# Full image name
FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

echo -e "${BLUE}Building Docker image: ${FULL_IMAGE_NAME}${NC}"

# Check if Dockerfile exists
if [[ ! -f "${DOCKERFILE}" ]]; then
  echo -e "${RED}Error: Dockerfile not found at ${DOCKERFILE}${NC}"
  exit 1
fi

# Build the Docker image
docker build -t "${FULL_IMAGE_NAME}" -f "${DOCKERFILE}" "${PROJECT_ROOT}"

echo -e "${GREEN}Docker image built successfully: ${FULL_IMAGE_NAME}${NC}"

# Push the image if requested
if [[ "${PUSH_IMAGE}" == "true" ]]; then
  echo -e "${BLUE}Pushing Docker image to registry: ${FULL_IMAGE_NAME}${NC}"
  docker push "${FULL_IMAGE_NAME}"
  
  # Push additional tag if specified
  if [[ -n "${ADDITIONAL_TAG}" ]]; then
    ADDITIONAL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${ADDITIONAL_TAG}"
    echo -e "${BLUE}Tagging and pushing additional tag: ${ADDITIONAL_IMAGE_NAME}${NC}"
    docker tag "${FULL_IMAGE_NAME}" "${ADDITIONAL_IMAGE_NAME}"
    docker push "${ADDITIONAL_IMAGE_NAME}"
  fi
  
  echo -e "${GREEN}Docker image pushed successfully${NC}"
fi

echo -e "${GREEN}CI image build process completed successfully${NC}"
exit 0 