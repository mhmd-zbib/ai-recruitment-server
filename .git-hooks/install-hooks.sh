#!/bin/bash

echo "Installing Git hooks..."

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Copy pre-commit hook to .git/hooks directory
echo "Copying pre-commit hook..."
cp -f "${SCRIPT_DIR}/pre-commit" "${SCRIPT_DIR}/../.git/hooks/"

# Copy commit-msg hook to .git/hooks directory
echo "Copying commit-msg hook..."
cp -f "${SCRIPT_DIR}/commit-msg" "${SCRIPT_DIR}/../.git/hooks/"

# Ensure the hooks are executable 
echo "Making hooks executable..."
chmod +x "${SCRIPT_DIR}/../.git/hooks/pre-commit"
chmod +x "${SCRIPT_DIR}/../.git/hooks/commit-msg"

echo "Git hooks installed successfully!" 
