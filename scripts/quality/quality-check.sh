#!/bin/bash

# Script to run all code quality checks and fix common issues

# ANSI color codes for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
RESET='\033[0m'

echo -e "${BLUE}===================================================${RESET}"
echo -e "${BLUE}      Running HireSync Code Quality Checks         ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

ERRORS_FOUND=0
WARNINGS_FOUND=0

# Step 1: Check for compilation errors
echo -e "\n${MAGENTA}Step 1: Checking for compilation errors${RESET}"
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo -e "${RED}✘ Compilation failed. Fix code errors first.${RESET}"
    ERRORS_FOUND=$((ERRORS_FOUND + 1))
    # Don't exit immediately, let's collect more issues
else
    echo -e "${GREEN}✓ Compilation successful${RESET}"
fi

# Step 2: Apply Spotless formatting automatically
echo -e "\n${MAGENTA}Step 2: Applying code formatting with Spotless${RESET}"
mvn spotless:apply

if [ $? -ne 0 ]; then
    echo -e "${RED}✘ Spotless apply failed. Some files may have issues that couldn't be automatically fixed.${RESET}"
    ERRORS_FOUND=$((ERRORS_FOUND + 1))
else
    echo -e "${GREEN}✓ Code formatting applied successfully${RESET}"
fi

# Step 3: Verify formatting with Spotless check
echo -e "\n${MAGENTA}Step 3: Verifying code formatting${RESET}"
mvn spotless:check

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Spotless check failed. Some files still need formatting.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Code formatting is correct${RESET}"
fi

# Step 4: Run Checkstyle analysis
echo -e "\n${MAGENTA}Step 4: Running code style analysis with Checkstyle${RESET}"
mvn checkstyle:check

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Checkstyle found issues. Fix code style issues.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
    # Save detailed report for later
    mvn checkstyle:checkstyle
else
    echo -e "${GREEN}✓ Code style checks passed${RESET}"
fi

# Step 5: Run PMD static code analysis
echo -e "\n${MAGENTA}Step 5: Running static code analysis with PMD${RESET}"
mvn pmd:check

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ PMD found potential code issues.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
    # Save detailed report for later
    mvn pmd:pmd
else
    echo -e "${GREEN}✓ Static code analysis passed${RESET}"
fi

# Step 6: Run security analysis with SpotBugs
echo -e "\n${MAGENTA}Step 6: Running security and bug analysis with SpotBugs${RESET}"
# Add this to your pom.xml if it doesn't exist, then run:
mvn com.github.spotbugs:spotbugs-maven-plugin:check

if [ $? -ne 0 ]; then
    echo -e "${RED}✘ SpotBugs found potential bugs or security issues.${RESET}"
    ERRORS_FOUND=$((ERRORS_FOUND + 1))
else
    echo -e "${GREEN}✓ No potential bugs or security issues found${RESET}"
fi

# Step 7: Run tests
echo -e "\n${MAGENTA}Step 7: Running unit tests${RESET}"
mvn test -Dspring.profiles.active=test -q

if [ $? -ne 0 ]; then
    echo -e "${RED}✘ Some tests failed. Fix failing tests.${RESET}"
    ERRORS_FOUND=$((ERRORS_FOUND + 1))
else
    echo -e "${GREEN}✓ All tests passed${RESET}"
fi

# Step 8: Database schema validation (if available)
echo -e "\n${MAGENTA}Step 8: Validating database schema${RESET}"
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=validate" -Dspring-boot.run.profiles=test -q

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Database schema validation failed. Entity mappings may not match database schema.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Database schema is valid${RESET}"
fi

# Step 9: Check for dependency vulnerabilities
echo -e "\n${MAGENTA}Step 9: Checking for dependency vulnerabilities${RESET}"
mvn org.owasp:dependency-check-maven:check -DskipProvidedScope=true -DskipTestScope=true

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Dependency vulnerabilities found. Check dependency-check-report.html for details.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ No dependency vulnerabilities found${RESET}"
fi

# Summary
echo -e "\n${BLUE}===================================================${RESET}"
echo -e "${BLUE}                  SUMMARY                          ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

if [ $ERRORS_FOUND -eq 0 ] && [ $WARNINGS_FOUND -eq 0 ]; then
    echo -e "${GREEN}✓ All quality checks passed successfully!${RESET}"
else
    if [ $ERRORS_FOUND -gt 0 ]; then
        echo -e "${RED}✘ Found $ERRORS_FOUND critical issues that must be fixed.${RESET}"
    fi
    if [ $WARNINGS_FOUND -gt 0 ]; then
        echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND warnings that should be addressed.${RESET}"
    fi
    
    echo -e "\n${CYAN}Detailed reports:${RESET}"
    echo -e "  - Checkstyle: target/site/checkstyle.html"
    echo -e "  - PMD: target/site/pmd.html"
    echo -e "  - SpotBugs: target/spotbugsXml.xml"
    echo -e "  - Dependency Check: target/dependency-check-report.html"
    echo -e "  - Test Results: target/surefire-reports/"
fi

echo -e "\n${BLUE}===================================================${RESET}"

exit $ERRORS_FOUND 