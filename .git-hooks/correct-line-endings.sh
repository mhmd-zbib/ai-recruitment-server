#!/bin/bash

# Script to correct line endings for all files in the repository
# This ensures that the correct line endings are used based on file type
# and platform requirements as defined in .gitattributes

# ANSI color codes for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Correcting Line Endings ===${NC}"
echo

# Get the directory of this script and the repository root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo -e "Working in repository: ${YELLOW}${REPO_ROOT}${NC}"
echo

# Check if .gitattributes exists
if [ ! -f "${REPO_ROOT}/.gitattributes" ]; then
    echo -e "${YELLOW}Warning: .gitattributes file not found.${NC}"
    echo -e "Creating default .gitattributes file with standard line ending rules..."
    
    cat > "${REPO_ROOT}/.gitattributes" << EOF
# Set default behavior to automatically normalize line endings
* text=auto

# Explicitly declare text files to always be normalized and converted
# to native line endings on checkout
*.java text
*.html text
*.css text
*.js text
*.ts text
*.json text
*.xml text
*.yml text
*.yaml text
*.properties text
*.md text
*.txt text
*.sql text
*.sh text eol=lf
*.bat text eol=crlf
*.cmd text eol=crlf
mvnw text eol=lf
*.py text

# Binary files should not be modified
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.ico binary
*.pdf binary
*.jar binary
*.war binary
*.zip binary
*.tar.gz binary
EOF
    
    echo -e "${GREEN}✓ Created default .gitattributes file${NC}"
    echo
fi

# Make Git detect and convert CRLF/LF according to .gitattributes
echo -e "${BLUE}Applying .gitattributes line ending rules...${NC}"
git add --renormalize .

# Check if there are any files with changed line endings
if git diff --name-only --exit-code; then
    echo -e "${GREEN}✓ All files already have correct line endings.${NC}"
else
    echo -e "${YELLOW}The following files had their line endings corrected:${NC}"
    git diff --name-only | while read -r file; do
        echo -e "  - ${file}"
    done
    echo
    
    # Check for potential merge conflicts involving line endings
    if git ls-files -u | grep -q .; then
        echo -e "${RED}Warning: Detected merge conflicts that may be related to line endings.${NC}"
        echo -e "${YELLOW}Please resolve these conflicts before committing:${NC}"
        git ls-files -u | cut -f 2 | sort -u | while read -r file; do
            echo -e "  - ${RED}${file}${NC}"
        done
        echo
    fi
    
    echo -e "${YELLOW}Remember to commit these changes.${NC}"
fi

# Detect files with mixed line endings (files that have both CRLF and LF)
echo
echo -e "${BLUE}Checking for files with mixed line endings...${NC}"
MIXED_ENDINGS_COUNT=0
MIXED_ENDINGS_FILES=()

# Only check text files, skip binary files
find "${REPO_ROOT}" -type f -not -path "*/\.*" -not -path "*/node_modules/*" -not -path "*/target/*" |
    while read -r file; do
        # Skip known binary files to avoid processing them
        if file "${file}" | grep -q "binary data"; then
            continue
        fi
        
        # Check if file has mixed line endings
        if grep -q $'\r' "${file}" && grep -l -q -P "(?<!\r)\n" "${file}"; then
            MIXED_ENDINGS_FILES+=("${file}")
            MIXED_ENDINGS_COUNT=$((MIXED_ENDINGS_COUNT + 1))
            
            # Fix the mixed line endings based on file extension
            if [[ "${file}" == *.sh ]] || [[ "${file}" == *.py ]] || [[ "${file}" == mvnw ]]; then
                # Use LF for shell scripts, Python, and Maven wrapper
                sed -i 's/\r$//' "${file}"
                echo -e "  - ${YELLOW}${file}${NC} (converted to LF)"
            elif [[ "${file}" == *.bat ]] || [[ "${file}" == *.cmd ]]; then
                # Use CRLF for Windows batch files
                sed -i 's/\r*$/\r/' "${file}"
                echo -e "  - ${YELLOW}${file}${NC} (converted to CRLF)"
            else
                # Use native line endings for other text files
                # On Unix, this will be LF; on Windows, this will be CRLF
                if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "win32" ]]; then
                    sed -i 's/\r*$/\r/' "${file}"
                    echo -e "  - ${YELLOW}${file}${NC} (converted to CRLF - Windows detected)"
                else
                    sed -i 's/\r$//' "${file}"
                    echo -e "  - ${YELLOW}${file}${NC} (converted to LF - Unix/macOS detected)"
                fi
            fi
        fi
    done

if [ ${MIXED_ENDINGS_COUNT} -eq 0 ]; then
    echo -e "${GREEN}✓ No files with mixed line endings detected.${NC}"
else
    echo -e "${YELLOW}Fixed ${MIXED_ENDINGS_COUNT} files with mixed line endings.${NC}"
    echo -e "${YELLOW}Consider committing these changes.${NC}"
fi

# Ensure all shell scripts are executable
echo
echo -e "${BLUE}Making shell scripts executable...${NC}"
find "${REPO_ROOT}" -name "*.sh" -type f -exec chmod +x {} \;
find "${REPO_ROOT}/.git-hooks" -type f -not -name "*.bat" -not -name "*.md" -exec chmod +x {} \;
find "${REPO_ROOT}" -name "mvnw" -type f -exec chmod +x {} \;

# Fix permissions for scripts in specific directories
if [ -d "${REPO_ROOT}/scripts" ]; then
    find "${REPO_ROOT}/scripts" -type f -name "*.sh" -exec chmod +x {} \;
    echo -e "${GREEN}✓ Made scripts in 'scripts' directory executable${NC}"
fi

if [ -d "${REPO_ROOT}/bin" ]; then
    find "${REPO_ROOT}/bin" -type f -not -name "*.bat" -not -name "*.cmd" -exec chmod +x {} \;
    echo -e "${GREEN}✓ Made scripts in 'bin' directory executable${NC}"
fi

echo
echo -e "${GREEN}✓ Line ending correction completed successfully.${NC}"
echo -e "${GREEN}✓ All shell scripts are now executable.${NC}"

exit 0 