-- PostgreSQL Initialization Script
-- This script will run when the PostgreSQL container is first initialized

-- Create a read-only role for reporting
CREATE ROLE hiresync_readonly WITH 
  LOGIN
  NOSUPERUSER
  INHERIT
  NOCREATEDB
  NOCREATEROLE
  NOREPLICATION
  PASSWORD 'readonly_password';

-- Grant privileges
GRANT CONNECT ON DATABASE hiresync TO hiresync_readonly;
GRANT USAGE ON SCHEMA public TO hiresync_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO hiresync_readonly;

-- Ensure future tables also get the same permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO hiresync_readonly;

-- Set config parameters that require superuser
ALTER SYSTEM SET max_connections = '200';

-- Create application required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set application database settings
ALTER DATABASE hiresync SET timezone TO 'UTC'; 