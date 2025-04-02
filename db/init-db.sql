-- PostgreSQL initial setup script
-- This script will be executed when the PostgreSQL container starts for the first time

-- Use environment variables passed from Docker Compose
\connect ${POSTGRES_DB}

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS hiresync;

-- Set the search path
SET search_path TO hiresync, public;

-- Create read-only user role
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readonly_user') THEN
    CREATE ROLE readonly_user WITH LOGIN PASSWORD '${PGSQL_APP_READ_PASSWORD}';
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO readonly_user;
    GRANT USAGE ON SCHEMA hiresync TO readonly_user;
    GRANT SELECT ON ALL TABLES IN SCHEMA hiresync TO readonly_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync GRANT SELECT ON TABLES TO readonly_user;
  END IF;
END
$$;

-- Create read-write user role
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readwrite_user') THEN
    CREATE ROLE readwrite_user WITH LOGIN PASSWORD '${PGSQL_APP_WRITE_PASSWORD}';
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO readwrite_user;
    GRANT USAGE, CREATE ON SCHEMA hiresync TO readwrite_user;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA hiresync TO readwrite_user;
    GRANT USAGE ON ALL SEQUENCES IN SCHEMA hiresync TO readwrite_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO readwrite_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync GRANT USAGE ON SEQUENCES TO readwrite_user;
  END IF;
END
$$;

-- Grant permissions to the main user
GRANT ALL PRIVILEGES ON SCHEMA hiresync TO ${POSTGRES_USER};
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA hiresync TO ${POSTGRES_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA hiresync TO ${POSTGRES_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync GRANT ALL PRIVILEGES ON TABLES TO ${POSTGRES_USER};
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync GRANT ALL PRIVILEGES ON SEQUENCES TO ${POSTGRES_USER}; 