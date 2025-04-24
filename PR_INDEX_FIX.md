# Job Index Name Fix

## Problem
The `Job` entity has an index named `idx_job_created_by` defined on the `created_by_id` column. However, PostgreSQL logs show errors attempting to create this index on a non-existent `created_by` column:

```
ERROR: column "created_by" does not exist
STATEMENT: create index idx_job_created_by on jobs (created_by)
```

This indicates a mismatch between the entity definition and how Hibernate/PostgreSQL is attempting to create the index.

## Solution
We've renamed the index to be more explicit about the column it targets:

From:
```java
@Index(name = "idx_job_created_by", columnList = "created_by_id")
```

To:
```java
@Index(name = "idx_job_created_by_id", columnList = "created_by_id")
```

This naming convention clearly indicates which column the index is targeting and should prevent confusion during schema generation.

## Testing
All unit tests continue to pass with this change. No database migration scripts were added as requested, since we're not yet in production.

## Deployment Notes
When deploying to production, database administrators should:

1. Drop the incorrectly named index if it exists:
   ```sql
   DROP INDEX IF EXISTS idx_job_created_by;
   ```

2. Ensure the correctly named index exists:
   ```sql
   CREATE INDEX idx_job_created_by_id ON jobs (created_by_id);
   ```

These steps can be performed manually or as part of the deployment process. 