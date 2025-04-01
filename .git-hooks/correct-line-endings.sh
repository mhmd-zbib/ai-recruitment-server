#!/bin/bash

# Fast line ending normalization that minimizes unnecessary work
# This script is optimized for speed and efficiency in large codebases

# ANSI color codes for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Get the repository root
REPO_ROOT=$(git rev-parse --show-toplevel)

# Cache directory to avoid redundant work
CACHE_DIR="$REPO_ROOT/.git/hooks-cache/line-endings"
mkdir -p "$CACHE_DIR" 2>/dev/null

# Check if this is the first run or forced
FIRST_RUN=false
if [ ! -f "$CACHE_DIR/last_run" ] || [ "$1" = "--force" ]; then
  FIRST_RUN=true
  mkdir -p "$CACHE_DIR" 2>/dev/null
fi

# Function to log messages
log() {
  local LEVEL=$1
  local MSG=$2
  
  case $LEVEL in
    "INFO")
      echo -e "${BLUE}$MSG${NC}"
      ;;
    "SUCCESS")
      echo -e "${GREEN}✓ $MSG${NC}"
      ;;
    "WARN")
      echo -e "${YELLOW}⚠ $MSG${NC}"
      ;;
    "ERROR")
      echo -e "${RED}✗ $MSG${NC}"
      ;;
    *)
      echo -e "$MSG"
      ;;
  esac
}

# Only show header for interactive runs or forced
if [ -t 1 ] || [ "$1" = "--verbose" ] || [ "$1" = "--force" ]; then
  log "INFO" "Normalizing line endings (optimized)..."
fi

# Check if .gitattributes exists, create default if needed
if [ ! -f "$REPO_ROOT/.gitattributes" ]; then
  if [ "$FIRST_RUN" = "true" ]; then
    log "WARN" "Creating default .gitattributes file..."
    
    cat > "$REPO_ROOT/.gitattributes" << EOF
# Default line ending configuration
* text=auto

# Unix line endings
*.sh text eol=lf
*.bash text eol=lf
*.py text eol=lf
mvnw text eol=lf
gradlew text eol=lf

# Windows line endings
*.bat text eol=crlf
*.cmd text eol=crlf
*.ps1 text eol=crlf

# Text files
*.java text
*.xml text
*.json text
*.properties text
*.yml text
*.yaml text
*.md text
*.sql text
*.txt text
*.html text
*.css text
*.js text
*.ts text

# Binary files
*.jar binary
*.war binary
*.zip binary
*.tar.gz binary
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.pdf binary
EOF
    
    log "SUCCESS" "Created default .gitattributes file"
    # Stage the new file
    git add "$REPO_ROOT/.gitattributes" 2>/dev/null
  fi
fi

# Cache key for tracking changes to .gitattributes
GITATTR_HASH_FILE="$CACHE_DIR/gitattr_hash"
CURRENT_GITATTR_HASH=""

if [ -f "$REPO_ROOT/.gitattributes" ]; then
  CURRENT_GITATTR_HASH=$(git hash-object "$REPO_ROOT/.gitattributes" 2>/dev/null || echo "")
fi

# Check if .gitattributes has changed since last run
GITATTR_CHANGED=false
if [ -f "$GITATTR_HASH_FILE" ]; then
  PREV_HASH=$(cat "$GITATTR_HASH_FILE")
  if [ "$CURRENT_GITATTR_HASH" != "$PREV_HASH" ]; then
    GITATTR_CHANGED=true
  fi
else
  GITATTR_CHANGED=true
fi

# If first run, force full normalization
if [ "$FIRST_RUN" = "true" ]; then
  GITATTR_CHANGED=true
fi

# Only do expensive processing if needed
if [ "$GITATTR_CHANGED" = "true" ]; then
  # Apply .gitattributes line ending rules with Git
  log "INFO" "Applying line ending rules..."
  
  # This is the primary, efficient way to normalize line endings
  git add --renormalize . >/dev/null 2>&1

  # Check if there are any files with changed line endings
  CHANGED_FILES=$(git diff --name-only | wc -l)
  
  if [ "$CHANGED_FILES" -gt 0 ]; then
    log "SUCCESS" "Normalized line endings in $CHANGED_FILES files"
    
    # Only show detailed list in verbose mode
    if [ "$1" = "--verbose" ]; then
      log "INFO" "Files with normalized line endings:"
      git diff --name-only | while read -r file; do
        echo "  - $file"
      done
    fi
  else
    log "SUCCESS" "Line endings already normalized"
  fi
  
  # Save the .gitattributes hash to avoid redundant processing
  echo "$CURRENT_GITATTR_HASH" > "$GITATTR_HASH_FILE"
else
  # Skip expensive processing
  if [ -t 1 ] || [ "$1" = "--verbose" ]; then
    log "INFO" "Skipping full normalization (.gitattributes unchanged)"
  fi
fi

# Ensure shell scripts are executable (fast and important)
if [ "$(uname)" = "Darwin" ] || [ "$(uname)" = "Linux" ]; then
  # On macOS/Linux, only make important scripts executable
  find "$REPO_ROOT" -maxdepth 3 -name "*.sh" -type f -not -path "*/node_modules/*" -not -path "*/target/*" -exec chmod +x {} \; 2>/dev/null
  find "$REPO_ROOT/.git-hooks" -type f -not -name "*.bat" -not -name "*.cmd" -not -name "*.md" -exec chmod +x {} \; 2>/dev/null
  find "$REPO_ROOT" -maxdepth 2 -name "mvnw" -type f -exec chmod +x {} \; 2>/dev/null
  find "$REPO_ROOT" -maxdepth 2 -name "gradlew" -type f -exec chmod +x {} \; 2>/dev/null
fi

# Update timestamp of last run
date +%s > "$CACHE_DIR/last_run"

# Only show success message in interactive mode
if [ -t 1 ] || [ "$1" = "--verbose" ]; then
  log "SUCCESS" "Line ending correction completed successfully"
fi

exit 0 