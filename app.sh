#!/bin/bash

# Define the available commands in an array for easy future modification
commands=("test" "start")

# Navigate to the root of the project
cd "$(dirname "$0")" || exit 1

# Check if the first argument is a valid command
if [[ " ${commands[*]} " =~ " $1 " ]]; then
  # Execute the corresponding script
  ./scripts/$1.sh
else
  echo "Usage: $0 {${commands[*]}}"
  exit 1
fi
