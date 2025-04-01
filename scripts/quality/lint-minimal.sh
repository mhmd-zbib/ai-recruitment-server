#!/bin/bash

# Script to run minimal linting checks without requiring successful compilation

# ANSI color codes for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
RESET='\033[0m'

echo -e "${BLUE}===================================================${RESET}"
echo -e "${BLUE}      Running HireSync Minimal Linting Checks      ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

WARNINGS_FOUND=0

# Step 1: Apply Spotless formatting automatically
echo -e "\n${MAGENTA}Step 1: Applying code formatting with Spotless${RESET}"
mvn spotless:apply -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Spotless apply had issues. Some files may not be properly formatted.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Code formatting applied successfully${RESET}"
fi

# Step 2: Check for basic style issues that don't require compilation
echo -e "\n${MAGENTA}Step 2: Checking import organization${RESET}"
mvn pmd:check -Drules=category/java/errorprone/bestpractices.xml/UnusedImports,category/java/errorprone/bestpractices.xml/UnnecessaryImport -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

if [ $? -ne 0 ]; then
    echo -e "${YELLOW}⚠ Import issues found. Please organize imports.${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Imports look good${RESET}"
fi

# Step 3: Look for common code style issues with regex patterns
echo -e "\n${MAGENTA}Step 3: Looking for common code style issues${RESET}"

# Check for missing Lombok annotations in class files
MISSING_LOMBOK=0
for FILE in $(find src/main/java -name "*.java"); do
    # Look for files that use fields but no Lombok or getters
    HAS_FIELDS=$(grep -c "private " $FILE || true)
    HAS_LOMBOK=$(grep -c "@Data\\|@Getter\\|@Setter\\|@Builder\\|@AllArgsConstructor\\|@NoArgsConstructor" $FILE || true)
    HAS_GETTERS=$(grep -c "public .* get" $FILE || true)
    
    if [ $HAS_FIELDS -gt 0 ] && [ $HAS_LOMBOK -eq 0 ] && [ $HAS_GETTERS -eq 0 ]; then
        echo -e "${YELLOW}⚠ $FILE may be missing Lombok annotations or getters/setters${RESET}"
        MISSING_LOMBOK=$((MISSING_LOMBOK + 1))
    fi
done

if [ $MISSING_LOMBOK -gt 0 ]; then
    echo -e "${YELLOW}⚠ Found $MISSING_LOMBOK files that may be missing Lombok annotations${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Lombok annotations look good${RESET}"
fi

# Check for public fields (should be private with getters/setters)
PUBLIC_FIELDS=0
for FILE in $(find src/main/java -name "*.java"); do
    # Count public fields
    HAS_PUBLIC_FIELDS=$(grep -c "public [^(].*;" $FILE || true)
    
    if [ $HAS_PUBLIC_FIELDS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $FILE has public fields, consider making them private with getters/setters${RESET}"
        PUBLIC_FIELDS=$((PUBLIC_FIELDS + 1))
    fi
done

if [ $PUBLIC_FIELDS -gt 0 ]; then
    echo -e "${YELLOW}⚠ Found $PUBLIC_FIELDS files with public fields${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ No public fields found${RESET}"
fi

# Check for inconsistent line endings
echo -e "\n${MAGENTA}Step 4: Checking for inconsistent line endings${RESET}"
INCONSISTENT_ENDINGS=0
for FILE in $(find src/main/java -name "*.java"); do
    # Check if file has mixed line endings
    HAS_CRLF=$(grep -l $'\r' $FILE || true)
    HAS_LF=$(grep -l $'\n' $FILE | grep -v $'\r\n' || true)
    
    if [ -n "$HAS_CRLF" ] && [ -n "$HAS_LF" ]; then
        echo -e "${YELLOW}⚠ $FILE has inconsistent line endings${RESET}"
        INCONSISTENT_ENDINGS=$((INCONSISTENT_ENDINGS + 1))
    fi
done

if [ $INCONSISTENT_ENDINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠ Found $INCONSISTENT_ENDINGS files with inconsistent line endings${RESET}"
    WARNINGS_FOUND=$((WARNINGS_FOUND + 1))
else
    echo -e "${GREEN}✓ Line endings are consistent${RESET}"
fi

# Summary
echo -e "\n${BLUE}===================================================${RESET}"
echo -e "${BLUE}                  SUMMARY                          ${RESET}"
echo -e "${BLUE}===================================================${RESET}"

if [ $WARNINGS_FOUND -eq 0 ]; then
    echo -e "${GREEN}✓ All minimal linting checks passed successfully!${RESET}"
else
    echo -e "${YELLOW}⚠ Found $WARNINGS_FOUND areas that need improvement:${RESET}"
    echo -e "${CYAN}   • Run 'mvn spotless:apply' to fix code formatting${RESET}"
    echo -e "${CYAN}   • Organize imports and remove unused ones${RESET}"
    echo -e "${CYAN}   • Add missing Lombok annotations or getters/setters${RESET}"
    echo -e "${CYAN}   • Make public fields private and add getters/setters${RESET}"
    echo -e "${CYAN}   • Fix inconsistent line endings${RESET}"
fi

echo -e "\n${BLUE}===================================================${RESET}"

exit $WARNINGS_FOUND 