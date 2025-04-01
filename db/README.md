# HireSync Database

This directory contains the database initialization scripts and schema for the HireSync application.

## Files

- **init.sql**: The primary database initialization script that creates:
  - Application database roles with proper permissions
  - Schema and extension setup
  - Audit logging infrastructure
  - Helper functions for auditing

## Database Design

The HireSync database uses a PostgreSQL database with the following components:

### Database Roles

- `app_read`: Read-only access to application data
- `app_write`: Read/write access to application data

Role passwords are configured through environment variables or command-line parameters.

### Schema

- `hiresync`: Primary application schema containing all tables and functions

### Extensions Used

- `uuid-ossp`: UUID generation
- `pg_trgm`: Text search with trigrams for fuzzy matching
- `pgcrypto`: Cryptographic functions for secure data storage

### Audit System

The database includes a comprehensive audit system that:

- Automatically tracks all data changes (INSERT, UPDATE, DELETE)
- Records both old and new values for modified records
- Captures user ID, client IP, and user agent information
- Provides functions to easily add audit triggers to tables

## Using the Database

Use the application run script to manage the database:

```bash
# Start the database
./run db start

# Initialize the database schema
./run db init

# Check database status
./run db status

# Create a backup
./run db backup

# Restore from backup
./run db restore <backup-file>
```

## Connecting Manually

You can connect to the database using the following default credentials:

- **Host**: localhost
- **Port**: 5432
- **Database**: hiresync_db
- **Username**: postgres
- **Password**: postgres
- **JDBC URL**: jdbc:postgresql://localhost:5432/hiresync_db

Application accounts (app_read and app_write) are also available after initialization.

## Adding Audit Triggers

To add auditing to a new table:

```sql
-- Using the helper function
SELECT hiresync.add_audit_trigger('hiresync.your_table');

-- Manual approach
CREATE TRIGGER audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON hiresync.your_table
FOR EACH ROW EXECUTE FUNCTION hiresync.audit_trigger_func();
```

## Key Tables

- **audit_log**: Tracks all data changes across tables
  - Automatically logs inserts, updates, and deletes
  - Stores before and after values for each change
  - Records user who made the change

## Docker Container

The database runs in a Docker container named `hiresync-postgres` using the PostgreSQL 14 Alpine image. 