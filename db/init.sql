-- HireSync Database Schema Initialization
-- This script initializes the database schema for the HireSync application.
-- It creates roles, schemas, and base tables required for the application.

-- Create roles with passwords from environment variables or use defaults
DO $$
DECLARE
  db_name text;
BEGIN
  -- Get the current database name
  SELECT current_database() INTO db_name;

  -- Get passwords from environment variables (or use defaults)
  PERFORM set_config('app.read_password', coalesce(current_setting('app.read_password', true), 'read_password'), false);
  PERFORM set_config('app.write_password', coalesce(current_setting('app.write_password', true), 'write_password'), false);
  
  -- Create roles with dynamic passwords
  EXECUTE format('CREATE ROLE app_read WITH LOGIN PASSWORD %L', current_setting('app.read_password'));
  EXECUTE format('CREATE ROLE app_write WITH LOGIN PASSWORD %L', current_setting('app.write_password'));
EXCEPTION
  WHEN duplicate_object THEN
    RAISE NOTICE 'Roles already exist, skipping creation';
END
$$;

-- Grant necessary permissions
DO $$
DECLARE
  db_name text;
BEGIN
  -- Get the current database name
  SELECT current_database() INTO db_name;
  
  -- Grant permissions using the current database name
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_read', db_name);
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_write', db_name);
END
$$;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";   -- UUID generation
CREATE EXTENSION IF NOT EXISTS "pg_trgm";     -- Trigram text search
CREATE EXTENSION IF NOT EXISTS "pgcrypto";    -- Cryptographic functions

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS hiresync;

-- Grant schema permissions
GRANT USAGE ON SCHEMA hiresync TO app_read;
GRANT USAGE, CREATE ON SCHEMA hiresync TO app_write;

-- Grant table permissions (will apply to future tables as well)
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync
    GRANT SELECT ON TABLES TO app_read;
    
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_write;

-- Grant sequence permissions
ALTER DEFAULT PRIVILEGES IN SCHEMA hiresync
    GRANT USAGE ON SEQUENCES TO app_write;

-- Create audit infrastructure
CREATE TABLE IF NOT EXISTS hiresync.audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    record_id TEXT NOT NULL,
    old_data JSONB,
    new_data JSONB,
    changed_by TEXT,
    client_ip INET,
    user_agent TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON hiresync.audit_log (table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON hiresync.audit_log (operation);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON hiresync.audit_log (changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_by ON hiresync.audit_log (changed_by);
CREATE INDEX IF NOT EXISTS idx_audit_log_record_id ON hiresync.audit_log (record_id);

-- Add comment to document the table
COMMENT ON TABLE hiresync.audit_log IS 'Tracks changes to important tables for auditing purposes';
COMMENT ON COLUMN hiresync.audit_log.old_data IS 'Previous state of the record';
COMMENT ON COLUMN hiresync.audit_log.new_data IS 'New state of the record';
COMMENT ON COLUMN hiresync.audit_log.client_ip IS 'IP address of the client that made the change';
COMMENT ON COLUMN hiresync.audit_log.user_agent IS 'User agent of the client that made the change';

-- Grant permissions on the audit table
GRANT SELECT ON hiresync.audit_log TO app_read;
GRANT SELECT, INSERT ON hiresync.audit_log TO app_write;

-- Create audit trigger function
CREATE OR REPLACE FUNCTION hiresync.audit_trigger_func()
RETURNS TRIGGER AS $$
DECLARE
    audit_data hiresync.audit_log;
BEGIN
    -- Set common audit fields
    audit_data.table_name := TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME;
    audit_data.changed_at := CURRENT_TIMESTAMP;
    
    -- Get user ID from application context or use 'system' if not available
    BEGIN
        audit_data.changed_by := current_setting('app.user_id');
    EXCEPTION WHEN OTHERS THEN
        audit_data.changed_by := 'system';
    END;
    
    -- Try to get client IP and user agent
    BEGIN
        audit_data.client_ip := inet(current_setting('app.client_ip', true));
    EXCEPTION WHEN OTHERS THEN
        audit_data.client_ip := NULL;
    END;
    
    BEGIN
        audit_data.user_agent := current_setting('app.user_agent', true);
    EXCEPTION WHEN OTHERS THEN
        audit_data.user_agent := NULL;
    END;
    
    IF (TG_OP = 'DELETE') THEN
        audit_data.operation := 'DELETE';
        audit_data.record_id := COALESCE(OLD.id::TEXT, 'unknown');
        audit_data.old_data := row_to_json(OLD);
        
        INSERT INTO hiresync.audit_log 
            (table_name, operation, record_id, old_data, changed_by, client_ip, user_agent, changed_at)
        VALUES 
            (audit_data.table_name, audit_data.operation, audit_data.record_id, 
             audit_data.old_data, audit_data.changed_by, audit_data.client_ip, audit_data.user_agent, audit_data.changed_at);
        
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        audit_data.operation := 'UPDATE';
        audit_data.record_id := COALESCE(NEW.id::TEXT, 'unknown');
        audit_data.old_data := row_to_json(OLD);
        audit_data.new_data := row_to_json(NEW);
        
        -- Only create an audit record if data actually changed
        IF audit_data.old_data = audit_data.new_data THEN
            RETURN NEW;
        END IF;
        
        INSERT INTO hiresync.audit_log 
            (table_name, operation, record_id, old_data, new_data, changed_by, client_ip, user_agent, changed_at)
        VALUES 
            (audit_data.table_name, audit_data.operation, audit_data.record_id,
             audit_data.old_data, audit_data.new_data, audit_data.changed_by, audit_data.client_ip, audit_data.user_agent, audit_data.changed_at);
        
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        audit_data.operation := 'INSERT';
        audit_data.record_id := COALESCE(NEW.id::TEXT, 'unknown');
        audit_data.new_data := row_to_json(NEW);
        
        INSERT INTO hiresync.audit_log 
            (table_name, operation, record_id, new_data, changed_by, client_ip, user_agent, changed_at)
        VALUES 
            (audit_data.table_name, audit_data.operation, audit_data.record_id,
             audit_data.new_data, audit_data.changed_by, audit_data.client_ip, audit_data.user_agent, audit_data.changed_at);
        
        RETURN NEW;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Add security barrier to prevent information leakage
REVOKE ALL ON FUNCTION hiresync.audit_trigger_func() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION hiresync.audit_trigger_func() TO app_write;

COMMENT ON FUNCTION hiresync.audit_trigger_func() IS 'Generic audit trigger function that logs changes to tables';

-- Create helper function to apply audit trigger to a table
CREATE OR REPLACE FUNCTION hiresync.add_audit_trigger(target_table text)
RETURNS VOID AS $$
BEGIN
    EXECUTE format('
        CREATE TRIGGER audit_trigger
        AFTER INSERT OR UPDATE OR DELETE ON %s
        FOR EACH ROW EXECUTE FUNCTION hiresync.audit_trigger_func();
    ', target_table);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Revoke public access and grant only to those who need it
REVOKE ALL ON FUNCTION hiresync.add_audit_trigger(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION hiresync.add_audit_trigger(text) TO app_write;

COMMENT ON FUNCTION hiresync.add_audit_trigger(text) IS 'Helper function to add audit trigger to a table'; 