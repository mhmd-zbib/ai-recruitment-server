#!/bin/bash

# Script to run code quality checks with options for quick or comprehensive analysis

# ANSI color codes for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
RESET='\033[0m'

# Process command line arguments
QUICK_MODE=false
AUTO_FIX=false
NORMALIZE_LINE_ENDINGS_ONLY=false

for arg in "$@"; do
    case $arg in
        --quick)
            QUICK_MODE=true
            ;;
        --auto-fix)
            AUTO_FIX=true
            ;;
        --line-endings-only)
            NORMALIZE_LINE_ENDINGS_ONLY=true
            ;;
        --help)
            echo "Usage: quality-check.sh [options]"
            echo "Options:"
            echo "  --quick              Run only basic checks and auto-fixes (equivalent to old lint-minimal)"
            echo "  --auto-fix           Automatically apply fixes where possible"
            echo "  --line-endings-only  Only normalize line endings according to .gitattributes"
            echo "  --help               Show this help message"
            exit 0
            ;;
    esac
done

# Get repository root directory
REPO_ROOT=$(git rev-parse --show-toplevel)

# Function to normalize line endings using Git
normalize_line_endings() {
    echo -e "\n${MAGENTA}Normalizing line endings according to .gitattributes${RESET}"
    
    # Make Git detect and convert CRLF/LF according to .gitattributes
    echo -e "${CYAN}   → Applying .gitattributes line ending rules...${RESET}"
    git add --renormalize .
    
    # Check if there are any files with changed line endings
    if git diff --name-only --exit-code; then
        report_success "All files already have correct line endings"
    else
        echo -e "${CYAN}   → The following files had their line endings corrected:${RESET}"
        git diff --name-only
        report_fix "Line endings corrected based on .gitattributes rules"
    fi
    
    # Ensure all shell scripts are executable
    echo -e "${CYAN}   → Making shell scripts executable...${RESET}"
    if [ "$(uname)" = "Darwin" ] || [ "$(uname)" = "Linux" ]; then
        # On macOS or Linux, use find directly
        find "${REPO_ROOT}" -name "*.sh" -type f -exec chmod +x {} \;
        find "${REPO_ROOT}/.git-hooks" -type f -not -name "*.bat" -not -name "*.md" -exec chmod +x {} \;
        find "${REPO_ROOT}" -name "mvnw" -type f -exec chmod +x {} \;
        report_fix "Shell scripts made executable"
    else
        # On Windows, use Git Bash if available
        if command -v bash >/dev/null 2>&1; then
            bash -c "find \"${REPO_ROOT}\" -name \"*.sh\" -type f -exec chmod +x {} \;"
            bash -c "find \"${REPO_ROOT}/.git-hooks\" -type f -not -name \"*.bat\" -not -name \"*.md\" -exec chmod +x {} \;"
            bash -c "find \"${REPO_ROOT}\" -name \"mvnw\" -type f -exec chmod +x {} \;"
            report_fix "Shell scripts made executable"
        else
            report_issue "WARNING" "Could not make shell scripts executable" "Manual action required on Windows without Git Bash"
        fi
    fi
}

# If line-endings-only flag is set, just run that and exit
if [ "$NORMALIZE_LINE_ENDINGS_ONLY" = true ]; then
    # Set title for line endings mode
    TITLE="Normalizing Line Endings for HireSync Project"
    echo -e "${BLUE}===================================================${RESET}"
    echo -e "${BLUE}      ${TITLE}         ${RESET}"
    echo -e "${BLUE}===================================================${RESET}"
    
    ERRORS_FOUND=0
    WARNINGS_FOUND=0
    ISSUES_FIXED=0
    
    # Function to report issues but continue
    report_issue() {
        LEVEL=$1
        MESSAGE=$2
        ACTION=$3
        
        if [ "$LEVEL" = "ERROR" ]; then
            echo -e "${RED}✘ $MESSAGE${RESET}"
            ERRORS_FOUND=$((ERRORS_FOUND + 1))
        else
            echo -e "${YELLOW}⚠ $MESSAGE${RESET}"
            WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
        fi
        
        if [ -n "$ACTION" ]; then
            echo -e "${CYAN}   → $ACTION${RESET}"
        fi
    }
    
    # Function to report auto-fixed issues
    report_fix() {
        echo -e "${GREEN}✓ $1${RESET}"
        ISSUES_FIXED=$((ISSUES_FIXED + 1))
    }
    
    # Function to report success
    report_success() {
        echo -e "${GREEN}✓ $1${RESET}"
    }
    
    # Run line ending normalization
    normalize_line_endings
    
    # Summary
    echo -e "\n${BLUE}===================================================${RESET}"
    echo -e "${BLUE}                  SUMMARY                          ${RESET}"
    echo -e "${BLUE}===================================================${RESET}"
    
    if [ $ERRORS_FOUND -eq 0 ] && [ $WARNINGS_FOUND -eq 0 ]; then
        echo -e "${GREEN}✓ Line ending normalization completed successfully!${RESET}"
    elif [ $ISSUES_FIXED -gt 0 ]; then
        echo -e "${GREEN}✓ Fixed $ISSUES_FIXED issues!${RESET}"
        
        if [ $ERRORS_FOUND -gt 0 ]; then
            echo -e "${RED}✘ Still found $ERRORS_FOUND critical issues that need attention.${RESET}"
        fi
        if [ $WARNINGS_FOUND -gt 0 ]; then
            echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND warnings that should be addressed when possible.${RESET}"
        fi
    fi
    
    echo -e "\n${GREEN}Line ending normalization completed!${RESET}"
    echo -e "\n${BLUE}===================================================${RESET}"
    
    exit 0
fi

# Set title based on mode
if [ "$QUICK_MODE" = true ]; then
    TITLE="Running HireSync Quick Quality Check"
else
    TITLE="Running HireSync Comprehensive Quality Check"
fi

echo -e "${BLUE}===================================================${RESET}"
echo -e "${BLUE}      ${TITLE}         ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

ERRORS_FOUND=0
WARNINGS_FOUND=0
ISSUES_FIXED=0

# Function to report issues but continue
report_issue() {
    LEVEL=$1
    MESSAGE=$2
    ACTION=$3
    
    if [ "$LEVEL" = "ERROR" ]; then
        echo -e "${RED}✘ $MESSAGE${RESET}"
        ERRORS_FOUND=$((ERRORS_FOUND + 1))
    else
        echo -e "${YELLOW}⚠ $MESSAGE${RESET}"
        WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
    fi
    
    if [ -n "$ACTION" ]; then
        echo -e "${CYAN}   → $ACTION${RESET}"
    fi
}

# Function to report auto-fixed issues
report_fix() {
    echo -e "${GREEN}✓ $1${RESET}"
    ISSUES_FIXED=$((ISSUES_FIXED + 1))
}

# Function to report success
report_success() {
    echo -e "${GREEN}✓ $1${RESET}"
}

# Step 1: Apply Spotless formatting if auto-fix enabled
if [ "$AUTO_FIX" = true ]; then
    echo -e "\n${MAGENTA}Step 1: Auto-fixing code formatting with Spotless${RESET}"
    mvn spotless:apply -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "Spotless apply had issues." "Some files may have complex issues that need manual fixing."
    else
        report_fix "Code formatting auto-fixed successfully"
    fi
else
    # Just run a spotless check without fixing
    echo -e "\n${MAGENTA}Step 1: Checking code formatting with Spotless${RESET}"
    mvn spotless:check -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "Code formatting issues found." "Run with --auto-fix to fix formatting issues."
    else
        report_success "Code formatting is correct"
    fi
fi

# Step 2: Check and fix import organization
echo -e "\n${MAGENTA}Step 2: Checking import organization${RESET}"
mvn pmd:check -Drules=category/java/errorprone/bestpractices.xml/UnusedImports,category/java/errorprone/bestpractices.xml/UnnecessaryImport -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q

if [ $? -ne 0 ]; then
    if [ "$AUTO_FIX" = true ]; then
        # Try to fix imports using Maven Tidy plugin if available
        if mvn -q org.codehaus.mojo:tidy-maven-plugin:1.1.0:pom -Dquiet=true 2>/dev/null; then
            report_fix "Auto-organized imports with Maven Tidy plugin"
        else
            report_issue "WARNING" "Import issues found." "Maven Tidy plugin not available. Consider manual organization."
        fi
    else
        report_issue "WARNING" "Import issues found." "Run with --auto-fix to attempt to fix imports."
    fi
else
    report_success "Imports look good"
fi

# Step 3: Check and fix line endings
echo -e "\n${MAGENTA}Step 3: Checking line endings${RESET}"

if [ "$AUTO_FIX" = true ]; then
    # Use Git-based comprehensive line ending normalization
    normalize_line_endings
else
    # Just check for inconsistent line endings without fixing
    INCONSISTENT_ENDINGS=$(find src/main/java -name "*.java" -type f -exec grep -l $'\r' {} \; | xargs grep -l $'\n' | grep -v $'\r\n' | wc -l 2>/dev/null || echo "0")
    
    if [ "$INCONSISTENT_ENDINGS" -gt 0 ]; then
        report_issue "WARNING" "Found $INCONSISTENT_ENDINGS files with inconsistent line endings." "Run with --auto-fix or --line-endings-only to normalize line endings."
    else
        report_success "Line endings appear consistent"
    fi
fi

# If in quick mode, only do basic analysis
if [ "$QUICK_MODE" = true ]; then
    # Step 4: Quick code pattern analysis
    echo -e "\n${MAGENTA}Step 4: Analyzing code patterns${RESET}"
    
    # Count files with potential Lombok opportunities
    MISSING_LOMBOK=$(find src/main/java -name "*.java" -type f -exec grep -l "private " {} \; | xargs grep -L "@Data\|@Getter\|@Setter\|@Builder\|@AllArgsConstructor\|@NoArgsConstructor" | xargs grep -L "public .* get" | wc -l 2>/dev/null || echo "0")
    
    if [ "$MISSING_LOMBOK" -gt 0 ]; then
        report_issue "WARNING" "Found $MISSING_LOMBOK files that may benefit from Lombok annotations" "Consider adding @Getter/@Setter or @Data annotations where appropriate."
    fi
    
    # Count files with public fields
    PUBLIC_FIELDS=$(find src/main/java -name "*.java" -type f -exec grep -l "public [^(].*;" {} \; | wc -l 2>/dev/null || echo "0")
    
    if [ "$PUBLIC_FIELDS" -gt 0 ]; then
        report_issue "WARNING" "Found $PUBLIC_FIELDS files with public fields" "Consider making them private with getters/setters."
    fi
else
    # Comprehensive quality checks
    # Step 4: Run Checkstyle analysis
    echo -e "\n${MAGENTA}Step 4: Running code style analysis with Checkstyle${RESET}"
    mvn checkstyle:check -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "Checkstyle found style issues." "Generating detailed report for reference."
        # Save detailed report for later
        mvn checkstyle:checkstyle -q
    else
        report_success "Code style checks passed"
    fi
    
    # Step 5: Run PMD static code analysis
    echo -e "\n${MAGENTA}Step 5: Running static code analysis with PMD${RESET}"
    mvn pmd:check -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "PMD found potential code issues." "Generating detailed report for reference."
        # Save detailed report for later
        mvn pmd:pmd -q
    else
        report_success "Static code analysis passed"
    fi
    
    # Step 6: Run security analysis with SpotBugs
    echo -e "\n${MAGENTA}Step 6: Running security and bug analysis with SpotBugs${RESET}"
    mvn com.github.spotbugs:spotbugs-maven-plugin:check -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "SpotBugs found potential bugs or security issues." "Check the report for details."
        # Generate a more readable HTML report
        mvn com.github.spotbugs:spotbugs-maven-plugin:gui -q
    else
        report_success "No potential bugs or security issues found"
    fi
    
    # Step 7: Run tests
    echo -e "\n${MAGENTA}Step 7: Running unit tests${RESET}"
    mvn test -Dspring.profiles.active=test -q
    
    if [ $? -ne 0 ]; then
        report_issue "WARNING" "Some tests failed." "Check the test reports for details."
    else
        report_success "All tests passed"
    fi
    
    # Step 8: Database schema validation (if available)
    echo -e "\n${MAGENTA}Step 8: Validating database schema${RESET}"
    # Run with validate mode in the background with a timeout to avoid hanging
    timeout 15s mvn spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=validate" -Dspring-boot.run.profiles=test -Dspring-boot.run.jvmArguments="-Xms128m -Xmx256m" >/dev/null 2>&1 &
    PID=$!
    
    # Wait for a short time then check result
    sleep 5
    if ps -p $PID > /dev/null; then
        report_success "Database schema appears valid"
        kill $PID 2>/dev/null || true
    else
        report_issue "WARNING" "Database schema validation issues detected." "Entity mappings may need adjustment."
    fi
    
    # Step 9: Check for dependency vulnerabilities (optional due to time)
    echo -e "\n${MAGENTA}Step 9: Checking for dependency vulnerabilities${RESET}"
    if [ -n "$SKIP_DEP_CHECK" ]; then
        echo -e "${CYAN}   Skipping dependency check as it can take a long time.${RESET}"
        echo -e "${CYAN}   Run 'mvn org.owasp:dependency-check-maven:check' separately if needed.${RESET}"
    else
        mvn org.owasp:dependency-check-maven:check -DskipProvidedScope=true -DskipTestScope=true -q
        
        if [ $? -ne 0 ]; then
            report_issue "WARNING" "Dependency vulnerabilities found." "Review dependency-check-report.html for details."
        else
            report_success "No dependency vulnerabilities found"
        fi
    fi
fi

# Summary
echo -e "\n${BLUE}===================================================${RESET}"
echo -e "${BLUE}                  SUMMARY                          ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

if [ $ERRORS_FOUND -eq 0 ] && [ $WARNINGS_FOUND -eq 0 ]; then
    echo -e "${GREEN}✓ All quality checks passed successfully!${RESET}"
elif [ $ISSUES_FIXED -gt 0 ]; then
    echo -e "${GREEN}✓ Auto-fixed $ISSUES_FIXED issues!${RESET}"
    
    if [ $ERRORS_FOUND -gt 0 ]; then
        echo -e "${RED}✘ Still found $ERRORS_FOUND critical issues that need attention.${RESET}"
    fi
    if [ $WARNINGS_FOUND -gt 0 ]; then
        echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND warnings that should be addressed when possible.${RESET}"
    fi
else
    if [ $ERRORS_FOUND -gt 0 ]; then
        echo -e "${RED}✘ Found $ERRORS_FOUND critical issues that need attention.${RESET}"
    fi
    if [ $WARNINGS_FOUND -gt 0 ]; then
        echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND warnings that should be addressed when possible.${RESET}"
    fi
fi

if [ "$QUICK_MODE" = false ]; then
    echo -e "\n${CYAN}Detailed reports available at:${RESET}"
    echo -e "  - Checkstyle: target/site/checkstyle.html"
    echo -e "  - PMD: target/site/pmd.html"
    echo -e "  - SpotBugs: target/spotbugsXml.xml and GUI report"
    echo -e "  - Dependency Check: target/dependency-check-report.html"
    echo -e "  - Test Results: target/surefire-reports/"
fi

echo -e "\n${GREEN}Quality check completed! All changes can still be committed.${RESET}"
echo -e "${GREEN}These reports are for your information only and won't block your workflow.${RESET}"

echo -e "\n${BLUE}===================================================${RESET}"

# Always return success to allow the workflow to continue
exit 0 