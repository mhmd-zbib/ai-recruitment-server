-- HireSync Database Initialization
-- This script will create the user and database specified in the .env file

-- Create application user if not exists
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = '${DB_USER}') THEN

      CREATE USER "${DB_USER}" WITH
        LOGIN
        NOSUPERUSER
        INHERIT
        CREATEDB
        NOCREATEROLE
        NOREPLICATION
        PASSWORD '${DB_PASSWORD}';
      
      RAISE NOTICE 'User "${DB_USER}" created';
   ELSE
      -- Update password if user exists
      ALTER USER "${DB_USER}" WITH PASSWORD '${DB_PASSWORD}';
      RAISE NOTICE 'User "${DB_USER}" already exists, password updated';
   END IF;
END
$do$;

-- Create database if not exists
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_database
      WHERE datname = '${DB_NAME}') THEN
      
      -- Create database with simple settings to avoid locale issues
      CREATE DATABASE "${DB_NAME}" WITH 
        OWNER = "${DB_USER}"
        ENCODING = 'UTF8'
        TEMPLATE = template0;
      
      RAISE NOTICE 'Database "${DB_NAME}" created';
   ELSE
      -- If database exists, make sure owner is set correctly
      ALTER DATABASE "${DB_NAME}" OWNER TO "${DB_USER}";
      RAISE NOTICE 'Database "${DB_NAME}" already exists, owner updated';
   END IF;
END
$do$;

-- Grant privileges on database
GRANT ALL PRIVILEGES ON DATABASE "${DB_NAME}" TO "${DB_USER}";

-- Connect to the database to grant schema privileges
\connect "${DB_NAME}";

-- Grant privileges on schema
GRANT ALL PRIVILEGES ON SCHEMA public TO "${DB_USER}";
ALTER USER "${DB_USER}" SET search_path TO public;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Log completion
\echo 'Database initialization complete.' 