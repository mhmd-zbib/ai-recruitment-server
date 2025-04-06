-- HireSync Database Initialization Script
-- This script runs when the PostgreSQL container is first created
-- It will set up the initial database schema and permissions

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Set timezone
SET timezone = 'UTC';

-- Create application schemas if they don't exist
CREATE SCHEMA IF NOT EXISTS hiresync;

-- Comment on schemas
COMMENT ON SCHEMA hiresync IS 'HireSync application schema for storing recruitment data';

-- Set default privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync 
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO current_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync
GRANT SELECT, UPDATE ON SEQUENCES TO current_user;

-- Create basic audit function
CREATE OR REPLACE FUNCTION hiresync.update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.modified_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create a sample function to validate emails (can be used in triggers)
CREATE OR REPLACE FUNCTION hiresync.is_valid_email(email TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN email ~* '^[A-Za-z0-9._%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+$';
END;
$$ LANGUAGE plpgsql; 