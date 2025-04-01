#!/bin/bash
# HireSync Development Environment Setup
# Sets up all necessary components for local development

# Source core utilities
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="$(cd "${SCRIPT_DIR}/../core" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

source "${CORE_DIR}/logging.sh"
source "${CORE_DIR}/env.sh"
source "${SCRIPT_DIR}/../utils/db.sh"

# Setup development environment
setup_dev_environment() {
  log_step "Setting up development environment"
  
  # Ensure proper environment file exists
  if [[ ! -f "${PROJECT_ROOT}/.env" ]]; then
    log_info "Creating new environment file"
    create_env_file "${PROJECT_ROOT}/.env"
  else
    log_info "Using existing environment file"
  fi
  
  # Load environment variables
  load_env "${PROJECT_ROOT}/.env"
  
  # Ensure database is set up
  log_info "Setting up database"
  db_setup || return 1
  
  # Check for Maven wrapper
  if [[ ! -f "${PROJECT_ROOT}/mvnw" ]]; then
    log_info "Setting up Maven wrapper"
    cd "$PROJECT_ROOT"
    
    # Check if Maven is installed
    if command -v mvn &> /dev/null; then
      mvn wrapper:wrapper -Dmaven=3.8.6
    else
      log_error "Maven is not installed, can't set up wrapper"
      log_info "Please install Maven or manually download the wrapper"
      return 1
    fi
  else
    log_info "Maven wrapper already exists"
    chmod +x "${PROJECT_ROOT}/mvnw"
  fi
  
  # Download dependencies
  log_info "Downloading dependencies"
  cd "$PROJECT_ROOT"
  ./mvnw dependency:go-offline -DskipTests
  
  # Set up Git hooks if we're in a Git repository
  if [[ -d "${PROJECT_ROOT}/.git" ]]; then
    log_info "Setting up Git hooks"
    setup_git_hooks
  fi
  
  # Set up IDE configuration
  log_info "Setting up IDE configuration"
  setup_ide_config
  
  log_info "Development environment setup completed"
  return 0
}

# Set up Git hooks for development workflow
setup_git_hooks() {
  local hooks_dir="${PROJECT_ROOT}/.git/hooks"
  
  # Create hooks directory if it doesn't exist
  mkdir -p "$hooks_dir"
  
  # Pre-commit hook to run checks
  cat > "${hooks_dir}/pre-commit" << 'EOF'
#!/bin/bash
echo "Running pre-commit checks..."

# Get changes staged for commit
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACMR | grep "\.java$")

if [[ "$STAGED_FILES" = "" ]]; then
  # No Java files staged, skip checks
  exit 0
fi

# Run formatter if available
if command -v ./mvnw &> /dev/null; then
  echo "Running formatter check..."
  ./mvnw spotless:check
  RESULT=$?
  if [[ $RESULT -ne 0 ]]; then
    echo "Formatter check failed! Run './mvnw spotless:apply' to fix formatting issues."
    exit 1
  fi
fi

# Add more checks here as needed
exit 0
EOF

  # Make hooks executable
  chmod +x "${hooks_dir}/pre-commit"
  
  log_info "Git hooks installed"
}

# Set up IDE configuration
setup_ide_config() {
  # Create basic VSCode settings
  local vscode_dir="${PROJECT_ROOT}/.vscode"
  mkdir -p "$vscode_dir"
  
  # Create launch.json for debugging
  if [[ ! -f "${vscode_dir}/launch.json" ]]; then
    cat > "${vscode_dir}/launch.json" << EOF
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Debug HireSync",
      "request": "launch",
      "mainClass": "com.zbib.hiresync.HireSyncApplication",
      "projectName": "hiresync",
      "env": {
        "SPRING_PROFILES_ACTIVE": "dev"
      },
      "args": []
    }
  ]
}
EOF
  fi

  # Create settings.json for consistent formatting
  if [[ ! -f "${vscode_dir}/settings.json" ]]; then
    cat > "${vscode_dir}/settings.json" << EOF
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": true
  },
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.format.enabled": true,
  "java.format.settings.url": "${PROJECT_ROOT}/.eclipse/formatter.xml",
  "java.format.settings.profile": "HireSync",
  "java.jdt.ls.vmargs": "-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -Dsun.zip.disableMemoryMapping=true -Xmx2G -Xms100m"
}
EOF
  fi
  
  log_info "IDE configuration set up"
}

# Main function
main() {
  # Parse command line arguments
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --quick)
        QUICK_SETUP=true
        shift
        ;;
      --help|-h)
        echo "Usage: ./run.sh dev:setup [options]"
        echo ""
        echo "Options:"
        echo "  --quick     Skip dependency downloads and other time-consuming steps"
        echo "  --help, -h  Show this help message"
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        exit 1
        ;;
    esac
  done
  
  # Run setup
  setup_dev_environment
  
  return $?
}

# Run main function if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi 