#!/bin/bash

# Health check script for HireSync application
# This is commonly used for monitoring, load balancers, and deployment validation

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
HEALTH_ENDPOINT=${HEALTH_ENDPOINT:-"/actuator/health"}
HOST=${HOST:-"localhost"}
PORT=${PORT:-"8080"}
TIMEOUT=${TIMEOUT:-"5"}
RETRIES=${RETRIES:-"3"}
DELAY=${DELAY:-"2"}
VERBOSE=false

# Print usage information
show_help() {
  echo "Health Check Script for HireSync"
  echo "Usage: $0 [options]"
  echo ""
  echo "Options:"
  echo "  --host HOST         Host to check (default: $HOST)"
  echo "  --port PORT         Port to use (default: $PORT)"
  echo "  --endpoint PATH     Health endpoint path (default: $HEALTH_ENDPOINT)"
  echo "  --timeout SECONDS   Connection timeout (default: $TIMEOUT)"
  echo "  --retries COUNT     Number of retry attempts (default: $RETRIES)"
  echo "  --delay SECONDS     Delay between retries (default: $DELAY)"
  echo "  --verbose           Enable verbose output"
  echo "  --help              Display this help message"
  echo ""
  echo "Examples:"
  echo "  $0 --host api.example.com --port 443"
  echo "  $0 --endpoint /api/v1/health --timeout 10"
}

# Process command line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      HOST="$2"
      shift 2
      ;;
    --port)
      PORT="$2"
      shift 2
      ;;
    --endpoint)
      HEALTH_ENDPOINT="$2"
      shift 2
      ;;
    --timeout)
      TIMEOUT="$2"
      shift 2
      ;;
    --retries)
      RETRIES="$2"
      shift 2
      ;;
    --delay)
      DELAY="$2"
      shift 2
      ;;
    --verbose)
      VERBOSE=true
      shift
      ;;
    --help)
      show_help
      exit 0
      ;;
    *)
      echo -e "${RED}Error: Unknown option: $1${NC}"
      show_help
      exit 1
      ;;
  esac
done

# Construct URL
URL="http://$HOST:$PORT$HEALTH_ENDPOINT"

# Log verbose information if requested
if [ "$VERBOSE" = true ]; then
  echo -e "${BLUE}Checking health at: $URL${NC}"
  echo -e "${BLUE}Timeout: $TIMEOUT seconds${NC}"
  echo -e "${BLUE}Retries: $RETRIES${NC}"
  echo -e "${BLUE}Delay between retries: $DELAY seconds${NC}"
fi

# Perform health check with retries
check_health() {
  local attempt=1
  local status_code
  local response
  
  while [ "$attempt" -le "$RETRIES" ]; do
    if [ "$VERBOSE" = true ]; then
      echo -e "${YELLOW}Attempt $attempt of $RETRIES...${NC}"
    fi
    
    # Use curl to get health status
    response=$(curl -s -o /dev/null -w "%{http_code}" -m "$TIMEOUT" "$URL" 2>/dev/null)
    status_code=$?
    
    # Check curl exit code
    if [ "$status_code" -eq 0 ]; then
      # Check HTTP status code
      if [ "$response" -eq 200 ]; then
        echo -e "${GREEN}✓ Health check succeeded! Service is up and healthy.${NC}"
        return 0
      else
        if [ "$VERBOSE" = true ]; then
          echo -e "${YELLOW}Received HTTP status code: $response${NC}"
        fi
      fi
    else
      if [ "$VERBOSE" = true ]; then
        echo -e "${YELLOW}Connection failed with exit code: $status_code${NC}"
      fi
    fi
    
    # Only sleep if we're going to retry
    if [ "$attempt" -lt "$RETRIES" ]; then
      if [ "$VERBOSE" = true ]; then
        echo -e "${YELLOW}Waiting $DELAY seconds before next attempt...${NC}"
      fi
      sleep "$DELAY"
    fi
    
    attempt=$((attempt + 1))
  done
  
  # All retries failed
  echo -e "${RED}✗ Health check failed after $RETRIES attempts.${NC}"
  echo -e "${RED}  Service at $URL is not responding or unhealthy.${NC}"
  return 1
}

# Execute health check
check_health
exit $? 