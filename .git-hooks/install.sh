#!/bin/bash
#
# Install Git hooks for HireSync project
# This script installs the hooks in .git-hooks to the .git/hooks directory

set -e

# Determine script and project directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GIT_DIR="$(cd "$PROJECT_ROOT" && git rev-parse --git-dir)"
HOOKS_DIR="$GIT_DIR/hooks"

# Colors for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========== HireSync Git Hooks Installer ==========${NC}"
echo -e "${BLUE}Installing Git hooks from: ${SCRIPT_DIR}${NC}"
echo -e "${BLUE}To Git hooks directory: ${HOOKS_DIR}${NC}"

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DIR"

# Get list of hooks to install
HOOKS=$(find "$SCRIPT_DIR" -type f -not -path "*/\.*" -not -name "install.sh" -not -name "install.cmd" -not -name "README.md")

# Install each hook
for HOOK_PATH in $HOOKS; do
    HOOK_NAME=$(basename "$HOOK_PATH")
    TARGET_PATH="$HOOKS_DIR/$HOOK_NAME"
    
    echo -e "${BLUE}Installing hook: ${HOOK_NAME}${NC}"
    
    # Copy hook file
    cp "$HOOK_PATH" "$TARGET_PATH"
    
    # Make executable
    chmod +x "$TARGET_PATH"
    
    echo -e "${GREEN}Successfully installed: ${HOOK_NAME}${NC}"
done

# Configure Git to use core.hooksPath
git config core.hooksPath "$HOOKS_DIR"

# Create a marker file instead of a symlink
echo "This file indicates that Git hooks are installed from $SCRIPT_DIR" > "$GIT_DIR/hooks.installed"

echo -e "${GREEN}Git hooks installation completed!${NC}"
echo -e "${YELLOW}Note: To bypass hooks temporarily, use: GIT_BYPASS_HOOKS=1 git commit/push${NC}"

exit 0 