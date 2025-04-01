#!/bin/bash

# Health Check Script for HireSync Application
# This script verifies that an endpoint is responding with a healthy status
# It supports configurable retry counts and delay periods
# Usage: ./health-check.sh --url https://example.com --retries 10 --delay 30

# Define ANSI color codes for better output readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
URL=""
HEALTH_ENDPOINT="/api/actuator/health"
MAX_RETRIES=10
DELAY_SECONDS=30
TIMEOUT_SECONDS=10
VERBOSE=false

# Function to display script usage
usage() {
    echo -e "${BLUE}Usage:${NC} $0 [options]"
    echo -e "Options:"
    echo -e "  --url URL             Base URL of the application (required)"
    echo -e "  --endpoint PATH       Health endpoint path (default: /api/actuator/health)"
    echo -e "  --retries N           Maximum number of retry attempts (default: 10)"
    echo -e "  --delay N             Delay between retries in seconds (default: 30)"
    echo -e "  --timeout N           Request timeout in seconds (default: 10)"
    echo -e "  --verbose             Enable verbose output"
    echo -e "  --help                Display this help message and exit"
    echo
    echo -e "Example: $0 --url https://example.com --retries 5 --delay 20"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --url)
            URL="$2"
            shift 2
            ;;
        --endpoint)
            HEALTH_ENDPOINT="$2"
            shift 2
            ;;
        --retries)
            MAX_RETRIES="$2"
            shift 2
            ;;
        --delay)
            DELAY_SECONDS="$2"
            shift 2
            ;;
        --timeout)
            TIMEOUT_SECONDS="$2"
            shift 2
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            echo -e "${RED}Error: Unknown option '$1'${NC}"
            usage
            ;;
    esac
done

# Validate required parameters
if [ -z "$URL" ]; then
    echo -e "${RED}Error: URL parameter is required${NC}"
    usage
fi

# Remove trailing slash from URL if present
URL=${URL%/}

# Full health endpoint URL
HEALTH_URL="${URL}${HEALTH_ENDPOINT}"

# Function to check if curl is available
check_curl() {
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}Error: curl is not installed. Please install curl and try again.${NC}"
        exit 1
    fi
}

# Function to perform the health check
perform_health_check() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}Checking health at:${NC} $HEALTH_URL"
    fi
    
    # Use curl to check if the endpoint is healthy (with timeout)
    response=$(curl --write-out "%{http_code}" --silent --output /dev/null --max-time $TIMEOUT_SECONDS "$HEALTH_URL")
    exit_code=$?
    
    # Check for curl errors (like connection refused, timeout, etc.)
    if [ $exit_code -ne 0 ]; then
        if [ "$VERBOSE" = true ]; then
            echo -e "${YELLOW}curl failed with exit code $exit_code${NC}"
        fi
        return 1
    fi
    
    # Check for HTTP success status code (2xx)
    if [[ $response -ge 200 && $response -lt 300 ]]; then
        if [ "$VERBOSE" = true ]; then
            echo -e "${GREEN}Health check passed with status code:${NC} $response"
        fi
        return 0
    else
        if [ "$VERBOSE" = true ]; then
            echo -e "${YELLOW}Health check failed with status code:${NC} $response"
        fi
        return 1
    fi
}

# Main execution
echo -e "${BLUE}Starting health check for${NC} $URL"
echo -e "${BLUE}Maximum retries:${NC} $MAX_RETRIES, ${BLUE}Delay:${NC} $DELAY_SECONDS seconds"

# Check if curl is available
check_curl

# Perform health check with retries
attempt=0
while [ $attempt -lt $MAX_RETRIES ]; do
    attempt=$((attempt+1))
    echo -e "${BLUE}Health check attempt${NC} $attempt ${BLUE}of${NC} $MAX_RETRIES..."
    
    if perform_health_check; then
        echo -e "${GREEN}Health check successful!${NC} The application is up and running."
        exit 0
    fi
    
    # If we've reached max retries, exit with error
    if [ $attempt -eq $MAX_RETRIES ]; then
        echo -e "${RED}Health check failed after $MAX_RETRIES attempts${NC}"
        echo -e "${RED}The application may not be deployed correctly or is experiencing issues${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Health check failed. Waiting $DELAY_SECONDS seconds before next attempt...${NC}"
    sleep $DELAY_SECONDS
done 