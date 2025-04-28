#!/bin/bash

# This script creates a temporary directory with all the files needed for the Docker build
# and then runs the Docker build with the correct context

# Create a temporary directory
TEMP_DIR=$(mktemp -d)

# Copy the necessary files to the temporary directory
cp -r ../src $TEMP_DIR/ || echo "Warning: Could not copy src directory"

# Check if .mvn directory exists before copying
if [ -d "../.mvn" ]; then
    cp -r ../.mvn $TEMP_DIR/
else
    echo "Warning: .mvn directory not found"
    # Create empty .mvn directory
    mkdir -p $TEMP_DIR/.mvn
    echo "Created empty .mvn directory"
 fi

# Check if mvnw file exists before copying
if [ -f "../mvnw" ]; then
    cp ../mvnw $TEMP_DIR/
else
    echo "Warning: mvnw file not found"
    # Create empty mvnw file
    touch $TEMP_DIR/mvnw
    chmod +x $TEMP_DIR/mvnw
    echo "Created empty mvnw file"
fi

# Check if pom.xml file exists before copying
if [ -f "../pom.xml" ]; then
    cp ../pom.xml $TEMP_DIR/
else
    echo "Warning: pom.xml file not found"
    # Create empty pom.xml file
    touch $TEMP_DIR/pom.xml
    echo "Created empty pom.xml file"
fi

# Copy the Dockerfile to the temporary directory
cp Dockerfile $TEMP_DIR/

# Change to the temporary directory
cd $TEMP_DIR

# List the contents of the directory
ls -la

# Run the Docker build
docker build -t hiresync:test .

# Return to the original directory
cd -

# Clean up the temporary directory
rm -rf $TEMP_DIR
