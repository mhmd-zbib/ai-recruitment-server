#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GIT_DIR="$(git rev-parse --git-dir)"

# Make hooks executable
chmod +x "$SCRIPT_DIR/pre-commit"

# Create symbolic links to the hooks
ln -sf "$SCRIPT_DIR/pre-commit" "$GIT_DIR/hooks/pre-commit"

echo "✅ Git hooks installed successfully!"
echo "The following hooks are now active:"
echo "  - pre-commit: Runs quick code quality checks before committing"
echo ""
echo "To bypass hooks temporarily, use: git commit --no-verify" 