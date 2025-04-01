#!/bin/bash

# Auto-installation script for Git hooks (Unix/Linux/macOS version)
# This script will:
# 1. Install the hooks immediately
# 2. Set up auto-installation on clone/pull for future uses
# 3. Configure Git to ensure hooks are maintained across the team

echo
echo "=== Automatic Git Hook Setup ==="
echo

# Get the directory of this script and the repository root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# First, install the hooks immediately
echo "Installing Git hooks immediately..."

# Get the Git directory
GIT_DIR=$(git rev-parse --git-dir 2>/dev/null)

# Fallback if git command fails
if [ -z "$GIT_DIR" ]; then
    GIT_DIR="${REPO_ROOT}/.git"
fi

# Ensure the destination directory exists
HOOKS_DIR="${GIT_DIR}/hooks"
if [ ! -d "${HOOKS_DIR}" ]; then
    mkdir -p "${HOOKS_DIR}" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "Error: Failed to create hooks directory at ${HOOKS_DIR}"
        exit 1
    fi
fi

# List of hooks to install
HOOKS="pre-commit commit-msg prepare-commit-msg pre-push post-checkout pre-rebase"

# Install each hook
for hook in $HOOKS; do
    SOURCE="${SCRIPT_DIR}/${hook}"
    TARGET="${HOOKS_DIR}/${hook}"
    
    if [ ! -f "${SOURCE}" ]; then
        echo "Warning: Hook ${hook} not found in ${SCRIPT_DIR}"
        continue
    fi
    
    echo "Installing ${hook} hook..."
    
    # Copy the hook
    cp "${SOURCE}" "${TARGET}" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "Error: Failed to copy ${hook} hook to ${TARGET}"
        exit 1
    fi
    
    # Make it executable
    chmod +x "${TARGET}" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "Error: Failed to make ${hook} hook executable"
        exit 1
    fi
    
    echo "✓ ${hook} installed successfully"
done

# Set up post-merge hook to auto-update hooks on pull
AUTO_UPDATE_HOOK="${HOOKS_DIR}/post-merge"
echo "Setting up auto-update on git pull..."

# Create post-merge hook for auto-update on pull
cat > "${AUTO_UPDATE_HOOK}" << 'EOF'
#!/bin/bash
# Auto-update hooks when pulling from remote

# Check if .git-hooks directory changed
if git diff-tree -r --name-only --no-commit-id ORIG_HEAD HEAD | grep -q ".git-hooks/"; then
    echo
    echo "Git hooks have been updated in the repository."
    echo "Auto-updating your local hooks..."
    
    # Get the directory of the repository
    REPO_ROOT=$(git rev-parse --show-toplevel)
    
    # Run the hooks installation script
    if [ -f "${REPO_ROOT}/.git-hooks/auto-install.sh" ]; then
        bash "${REPO_ROOT}/.git-hooks/auto-install.sh"
    fi
fi
EOF

# Make the hook executable
chmod +x "${AUTO_UPDATE_HOOK}"

# Create a post-checkout hook for initial clone
POST_CHECKOUT_AUTO="${HOOKS_DIR}/post-checkout"
if [ ! -f "${POST_CHECKOUT_AUTO}" ]; then
    echo "Setting up auto-install on first clone..."
    
    # Create post-checkout script
    cat > "${POST_CHECKOUT_AUTO}" << 'EOF'
#!/bin/bash
# Auto-install hooks on initial clone

# Arguments passed to hook by Git
PREV_HEAD=$1
NEW_HEAD=$2
CHECKOUT_TYPE=$3

# Check if this is the initial clone (previous HEAD is all zeros)
if [ "$PREV_HEAD" = "0000000000000000000000000000000000000000" ]; then
    echo
    echo "First checkout detected. Installing Git hooks..."
    
    # Get the directory of the repository
    REPO_ROOT=$(git rev-parse --show-toplevel)
    
    # Run the hooks installation script
    if [ -f "${REPO_ROOT}/.git-hooks/auto-install.sh" ]; then
        bash "${REPO_ROOT}/.git-hooks/auto-install.sh"
    fi
fi

# Execute the actual post-checkout hook if it exists separately
if [ -f "${REPO_ROOT}/.git/hooks/post-checkout.actual" ]; then
    bash "${REPO_ROOT}/.git/hooks/post-checkout.actual" "$@"
fi
EOF
    
    # Make the hook executable
    chmod +x "${POST_CHECKOUT_AUTO}"
    
    # If a post-checkout hook already exists, rename it
    if [ -f "${REPO_ROOT}/.git-hooks/post-checkout" ]; then
        cp "${REPO_ROOT}/.git-hooks/post-checkout" "${REPO_ROOT}/.git/hooks/post-checkout.actual"
        chmod +x "${REPO_ROOT}/.git/hooks/post-checkout.actual"
    fi
fi

# Set up template directory for automatic hook installation
echo "Setting up hook templates for future clones..."
GIT_TEMPLATE_DIR="${REPO_ROOT}/.git-template"
mkdir -p "${GIT_TEMPLATE_DIR}/hooks" 2>/dev/null

# Create template post-checkout hook that auto-installs
cat > "${GIT_TEMPLATE_DIR}/hooks/post-checkout" << 'EOF'
#!/bin/bash
# Template hook to install project hooks on clone

# Get the repository root
REPO_ROOT=$(git rev-parse --show-toplevel)

# Check if this is the initial clone
if [ "$1" = "0000000000000000000000000000000000000000" ]; then
    echo "First checkout detected. Installing project-specific hooks..."
    
    # Run the hooks installation script if it exists
    if [ -f "${REPO_ROOT}/.git-hooks/auto-install.sh" ]; then
        bash "${REPO_ROOT}/.git-hooks/auto-install.sh"
    fi
fi

# Exit with success
exit 0
EOF

# Make the template hook executable
chmod +x "${GIT_TEMPLATE_DIR}/hooks/post-checkout"

# Configure Git to use the template directory
echo "Configuring Git template directory..."
git config --local init.templateDir "${GIT_TEMPLATE_DIR}"

# Add reminder to README if it doesn't already exist
echo "Updating README.md to include auto-install instructions..."
if [ -f "${REPO_ROOT}/README.md" ]; then
    if ! grep -q "Setup Git Hooks" "${REPO_ROOT}/README.md"; then
        cat >> "${REPO_ROOT}/README.md" << 'EOF'

## Setup Git Hooks

This repository uses Git hooks to ensure code quality and consistent commit messages.
Run the following command to automatically set up the hooks:

```bash
# For Unix/Linux/macOS
bash .git-hooks/auto-install.sh

# For Windows
.\.git-hooks\auto-install.bat
```

EOF
    fi
fi

echo
echo "✅ Git hooks auto-installation has been set up successfully!"
echo "Hooks will automatically install for new clones and stay updated on pulls."
echo "Note: Ask all team members to run this script once to enable these features."
echo

exit 0 