#!/bin/bash

# Streamlined git hooks installer for modern Git workflows
# Uses core.hooksPath for zero-overhead installation

# ANSI color codes for better readability
GREEN="\033[0;32m"
YELLOW="\033[0;33m"
RED="\033[0;31m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m"

echo -e "${BLUE}${BOLD}=== Git Hooks Setup - Senior Engineering Edition ===${NC}"

# Get the directory of this script and repository root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
HOOKS_DIR="$SCRIPT_DIR"

# Check if hooks directory is valid
if [ ! -d "$HOOKS_DIR" ] || [ ! -f "$HOOKS_DIR/pre-commit" ]; then
  echo -e "${RED}Error: Git hooks directory not found or incomplete at:${NC}"
  echo -e "${RED}$HOOKS_DIR${NC}"
  exit 1
fi

# Check git version to ensure core.hooksPath is supported
GIT_VERSION=$(git --version | awk '{print $3}')
if [ "$(printf '%s\n' "2.9.0" "$GIT_VERSION" | sort -V | head -n1)" = "2.9.0" ]; then
  # Modern Git with hooksPath support
  echo -e "${GREEN}✓ Git version $GIT_VERSION supports modern hooks installation${NC}"
  
  # Make all hooks executable
  find "$HOOKS_DIR" -type f -not -name "*.md" -not -name "*.bat" -not -name "README*" -exec chmod +x {} \;
  
  # Configure git to use the hooks directory directly
  git config core.hooksPath "$HOOKS_DIR"
  
  echo -e "${GREEN}✓ Hooks installed via core.hooksPath (zero performance overhead)${NC}"
  
  # Also update config to avoid issues with line endings
  git config core.autocrlf false
  git config core.eol native
  
  # Check if this is a shared project setup
  if [ -f "$REPO_ROOT/.git/config" ]; then
    echo -e "${BLUE}Setting up hooks for local repository...${NC}"
    
    # Update user's git configuration to simplify workflow
    # Allow skipping hooks when needed
    echo -e "${BLUE}Adding helpful git aliases...${NC}"
    
    # Add git aliases for bypassing hooks when needed
    git config --local alias.pushf "push --no-verify"
    git config --local alias.commitf "commit --no-verify"
    git config --local alias.bypass-hooks "!export GIT_BYPASS_HOOKS=true; git"
    
    echo -e "${GREEN}✓ Added helpful git aliases:${NC}"
    echo -e "  ${YELLOW}git pushf${NC} - Push without hook verification"
    echo -e "  ${YELLOW}git commitf${NC} - Commit without hook verification"
    echo -e "  ${YELLOW}git bypass-hooks <command>${NC} - Run any git command bypassing hooks"
  fi
else
  # Fall back to traditional symlink method for older Git versions
  echo -e "${YELLOW}⚠ Git version $GIT_VERSION is older than 2.9.0${NC}"
  echo -e "${YELLOW}Using compatibility mode (symlinks) for hook installation${NC}"
  
  # Get the Git hooks directory
  GIT_DIR=$(git rev-parse --git-dir)
  GIT_HOOKS_DIR="$GIT_DIR/hooks"
  
  # Create hooks directory if it doesn't exist
  mkdir -p "$GIT_HOOKS_DIR"
  
  # List of hooks to install
  HOOKS="pre-commit commit-msg prepare-commit-msg pre-push post-checkout pre-rebase"
  
  # Install each hook as a symlink to this directory
  for hook in $HOOKS; do
    if [ -f "$HOOKS_DIR/$hook" ]; then
      # Remove existing hook if it's a regular file or symlink
      if [ -f "$GIT_HOOKS_DIR/$hook" ] || [ -L "$GIT_HOOKS_DIR/$hook" ]; then
        rm -f "$GIT_HOOKS_DIR/$hook"
      fi
      
      # Create the symlink
      ln -sf "$HOOKS_DIR/$hook" "$GIT_HOOKS_DIR/$hook"
      chmod +x "$HOOKS_DIR/$hook"
      echo -e "${GREEN}✓ Installed $hook hook${NC}"
    fi
  done
fi

# Normalize line endings immediately
echo -e "${BLUE}Normalizing line endings...${NC}"
"$HOOKS_DIR/correct-line-endings.sh" --force

echo
echo -e "${GREEN}${BOLD}✅ Git hooks successfully installed!${NC}"
echo
echo -e "${YELLOW}Available environment variables to control hooks:${NC}"
echo -e "  ${BLUE}GIT_BYPASS_HOOKS=true${NC} - Bypass all hooks"
echo -e "  ${BLUE}SKIP_PUSH_HOOKS=true${NC} - Skip pre-push hooks only"
echo -e "  ${BLUE}FORCE_CHECKS=true${NC} - Force full checks even if unchanged"
echo -e "  ${BLUE}BYPASS_COMMIT_MSG_HOOK=true${NC} - Skip commit message validation"
echo

# Provide quick usage guide
echo -e "${BLUE}${BOLD}Quick Usage Guide:${NC}"
echo -e "  • Normal workflow: Just commit and push as usual"
echo -e "  • Bypass hooks: ${YELLOW}GIT_BYPASS_HOOKS=true git commit -m 'msg'${NC}"
echo -e "  • Skip for a single repo: ${YELLOW}git config --local hooks.enabled false${NC}"
echo

exit 0 