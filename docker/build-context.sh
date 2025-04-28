#!/bin/bash

# This script creates a temporary directory with all the files needed for the Docker build
# and then runs the Docker build with the correct context

# Create a temporary directory
TEMP_DIR=$(mktemp -d)

# Copy the necessary files to the temporary directory
cp -r ../src $TEMP_DIR/
cp -r ../.mvn $TEMP_DIR/
cp ../mvnw $TEMP_DIR/
cp ../pom.xml $TEMP_DIR/

# Copy the Dockerfile to the temporary directory
cp Dockerfile $TEMP_DIR/

# Change to the temporary directory
cd $TEMP_DIR

# Run the Docker build
docker build -t hiresync:test .

# Return to the original directory
cd -

# Clean up the temporary directory
rm -rf $TEMP_DIR
