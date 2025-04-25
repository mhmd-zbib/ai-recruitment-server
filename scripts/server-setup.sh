#!/bin/bash
#
# HireSync Server Setup Script
# This script automates the setup of servers for HireSync deployment
#

# Set strict mode
set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to display usage info
usage() {
    echo -e "${YELLOW}Usage:${NC} $0 [dev|prod]"
    echo -e "Sets up the server environment for HireSync deployment."
    echo -e "\n${YELLOW}Options:${NC}"
    echo -e "  dev   Setup for development environment (Digital Ocean)"
    echo -e "  prod  Setup for production environment (AWS EC2)"
    exit 1
}

# Function to display step information
step() {
    echo -e "\n${GREEN}=== $1 ===${NC}"
}

# Function to display error and exit
error() {
    echo -e "${RED}ERROR: $1${NC}" >&2
    exit 1
}

# Check arguments
if [ $# -ne 1 ]; then
    usage
fi

ENV=$1
if [[ "$ENV" != "dev" && "$ENV" != "prod" ]]; then
    usage
fi

# Check if running as root
if [ "$(id -u)" -eq 0 ]; then
    error "This script should not be run as root. Please run as a regular user with sudo privileges."
fi

# Main setup function
setup_server() {
    step "Setting up server for $ENV environment"
    
    # Update system packages
    step "Updating system packages"
    sudo apt-get update || sudo yum update -y
    
    # Install basic utilities
    step "Installing basic utilities"
    if command -v apt-get &> /dev/null; then
        sudo apt-get install -y curl wget git jq htop
    elif command -v yum &> /dev/null; then
        sudo yum install -y curl wget git jq htop
    else
        error "Unsupported package manager. This script supports apt-get and yum."
    fi
    
    # Install Docker
    step "Installing Docker"
    if ! command -v docker &> /dev/null; then
        if command -v apt-get &> /dev/null; then
            # Debian/Ubuntu
            sudo apt-get install -y apt-transport-https ca-certificates software-properties-common
            curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
            sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
            sudo apt-get update
            sudo apt-get install -y docker-ce docker-ce-cli containerd.io
        elif command -v amazon-linux-extras &> /dev/null; then
            # Amazon Linux
            sudo amazon-linux-extras install docker -y
            sudo yum install -y docker
        elif command -v yum &> /dev/null; then
            # CentOS/RHEL
            sudo yum install -y yum-utils
            sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
            sudo yum install -y docker-ce docker-ce-cli containerd.io
        fi
        
        # Start and enable Docker
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -aG docker $USER
        echo -e "${YELLOW}You may need to log out and back in for Docker group membership to take effect.${NC}"
    else
        echo "Docker is already installed."
    fi
    
    # Install Docker Compose
    step "Installing Docker Compose"
    if ! command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | jq -r '.tag_name')
        sudo curl -L "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
        echo "Docker Compose installed successfully."
    else
        echo "Docker Compose is already installed."
    fi
    
    # Create application directory
    step "Creating application directory"
    sudo mkdir -p /opt/hiresync
    sudo chown -R $USER:$USER /opt/hiresync
    
    # Clone repository
    step "Cloning repository"
    if [ ! -d "/opt/hiresync/.git" ]; then
        BRANCH="master"
        if [ "$ENV" = "dev" ]; then
            BRANCH="dev"
        fi
        
        cd /opt/hiresync
        git clone -b $BRANCH https://github.com/your-repo/hiresync.git .
        echo "Repository cloned successfully."
    else
        echo "Repository already exists, pulling latest changes..."
        cd /opt/hiresync
        git pull
    fi
    
    # Set up environment file
    step "Setting up .env file"
    if [ "$ENV" = "dev" ]; then
        cp .env.example .env
        echo "Created .env file from template. Please update with your actual values."
    else
        cp .env.prod.template .env
        echo "Created .env file from production template. Please update with your actual values."
    fi
    
    # Set up firewall
    step "Setting up firewall"
    if command -v ufw &> /dev/null; then
        # Ubuntu/Debian
        sudo ufw allow ssh
        sudo ufw allow 80/tcp
        sudo ufw allow 443/tcp
        sudo ufw allow 8080/tcp
        
        # Only enable if not already enabled, to avoid cutting the connection
        if ! sudo ufw status | grep -q "Status: active"; then
            echo "y" | sudo ufw enable
        fi
    elif command -v firewall-cmd &> /dev/null; then
        # CentOS/RHEL
        sudo firewall-cmd --permanent --add-service=ssh
        sudo firewall-cmd --permanent --add-service=http
        sudo firewall-cmd --permanent --add-service=https
        sudo firewall-cmd --permanent --add-port=8080/tcp
        sudo firewall-cmd --reload
    fi
    
    # Final instructions
    step "Setup completed successfully"
    echo -e "${GREEN}Server setup for $ENV environment completed successfully!${NC}"
    echo -e "Next steps:"
    echo -e "1. Update the .env file with your actual configuration values"
    echo -e "2. Configure your CI/CD pipeline to deploy to this server"
    echo -e "3. For manual deployment, use: docker-compose -f docker-compose.$ENV.yaml up -d"
    
    if [ "$(id -u)" -eq $(id -u $USER) ]; then
        echo -e "\n${YELLOW}Note:${NC} You may need to log out and back in for Docker group membership to take effect."
    fi
}

# Run the setup
setup_server 