# Integration Test Failure Resolution Plan

## Overview
This document outlines a plan to fix the integration test failures in the HireSync AI Recruitment Server application. The tests are failing with `ApplicationContext failure threshold exceeded` errors, indicating issues with the Spring Boot test configuration and database connectivity.

## Problem Analysis

### Error Symptoms
- All 17 integration tests are failing with the same error pattern
- Error: `ApplicationContext failure threshold (1) exceeded: skipping repeated attempt to load context`
- Tests affected: `com.zbib.hiresync.integration.JobServiceIntegrationTest` and potentially others
- Root cause: Spring Boot test context cannot be properly initialized

### Likely Causes
1. **Database Connection Issues**: 
   - The integration tests cannot connect to the PostgreSQL database
   - Connection URL in pipeline (`localhost:5432`) vs Docker Compose network (`postgres:5432`)

2. **Test Configuration Problems**:
   - Incorrect Spring profile activation
   - Insufficient wait time for database to be ready
   - Missing environment variables required by the application

3. **Resource Initialization Timing**:
   - Database container may not be fully ready when tests begin
   - Health check mechanism not properly utilized

## Action Plan

### 1. Fix CI Pipeline Database Connection Configuration
- [ ] **Check**: Review docker-compose.test.yaml network configuration
- [ ] **Check**: Review application-test.yaml database connection properties
- [ ] **Fix**: Update database connection URL in CI pipeline
  - From: `SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb`
  - To: `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/testdb`

### 2. Improve Service Readiness Mechanism
- [ ] **Check**: Verify PostgreSQL container health check configuration
- [ ] **Fix**: Replace sleep-based wait with polling for database readiness
  - Create helper script `scripts/wait-for-db.sh` to properly wait for database
  - Use PostgreSQL client (`pg_isready`) to check database availability

### 3. Enhance Test Environment Configuration
- [ ] **Check**: Validate all required environment variables for test context
- [ ] **Fix**: Create a comprehensive test environment file
  - Create `.env.test` file with all required variables
  - Use Docker Compose environment_file feature

### 4. Implement Proper Test Isolation
- [ ] **Check**: Review test class setup for proper use of @SpringBootTest
- [ ] **Fix**: Ensure test classes properly clean up resources
  - Add proper teardown methods using @AfterEach or @AfterAll
  - Consider using testcontainers for database isolation

## Implementation Details

### 1. Database Connection Fix

Update the integration-test job in pipeline.yaml:

```yaml
integration-test:
  runs-on: ubuntu-22.04
  needs: docker-build
  steps:
    # ...existing code...
    
    # Run integration tests with the Docker container
    - name: Run Integration Tests
      env:
        SPRING_PROFILES_ACTIVE: test
        SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/testdb
        SPRING_DATASOURCE_USERNAME: test
        SPRING_DATASOURCE_PASSWORD: test
        JWT_SECRET: test-secret-key-for-integration-tests-only
        JWT_ISSUER: test
        JWT_AUDIENCE: test
      run: |
        docker compose -f docker/docker-compose.test.yaml up -d
        
        # Use wait-for script instead of sleep
        ./scripts/wait-for-db.sh
        
        # Run only integration tests
        ./mvnw test -Dtest="com.zbib.hiresync.integration.**.*Test"
```

### 2. Database Readiness Script

Create a new file `scripts/wait-for-db.sh`:

```bash
#!/bin/bash
set -e

MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Waiting for PostgreSQL to be ready..."
retries=0
until docker exec test-db pg_isready -U test -d testdb || [ $retries -eq $MAX_RETRIES ]; do
  echo "PostgreSQL is unavailable - sleeping for ${RETRY_INTERVAL}s"
  sleep $RETRY_INTERVAL
  retries=$((retries+1))
done

if [ $retries -eq $MAX_RETRIES ]; then
  echo "Error: PostgreSQL did not become ready in time"
  docker logs test-db
  exit 1
fi

echo "PostgreSQL is up and running!"
```

Make it executable:
```bash
chmod +x scripts/wait-for-db.sh
```

### 3. Test Environment Configuration

Create `.env.test` file:

```
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=testdb
DB_USERNAME=test
DB_PASSWORD=test

# Application
SPRING_PROFILES_ACTIVE=test
SERVER_PORT=8080

# Security
JWT_SECRET=test-secret-key-for-integration-tests-only
JWT_EXPIRATION=3600000
JWT_ISSUER=test
JWT_AUDIENCE=test
JWT_REFRESH_EXPIRATION=86400000
```

Update docker-compose.test.yaml to use this file:

```yaml
services:
  postgres:
    # ...existing code...
  
  app:
    # ...existing code...
    env_file: 
      - .env.test
```

### 4. Test Class Configuration

Review and update test classes if needed:

```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JobServiceIntegrationTest {
    
    @Autowired
    private JobRepository jobRepository;
    
    @BeforeEach
    public void setup() {
        // Clear database state before each test
        jobRepository.deleteAll();
    }
    
    @AfterAll
    public void cleanup() {
        // Final cleanup
        jobRepository.deleteAll();
    }
    
    // Test methods...
}
```

## Additional Recommendations

1. **Use Testcontainers**: Consider migrating to Testcontainers to manage test database lifecycle:
   ```java
   @Testcontainers
   @SpringBootTest
   class JobServiceIntegrationTest {
       @Container
       static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
           .withDatabaseName("testdb")
           .withUsername("test")
           .withPassword("test");
   
       @DynamicPropertySource
       static void postgresProperties(DynamicPropertyRegistry registry) {
           registry.add("spring.datasource.url", postgres::getJdbcUrl);
           registry.add("spring.datasource.username", postgres::getUsername);
           registry.add("spring.datasource.password", postgres::getPassword);
       }
   }
   ```

2. **Optimize Test Speed**: Configure tests to run in parallel when possible by adding to pom.xml:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-surefire-plugin</artifactId>
       <configuration>
           <parallel>classes</parallel>
           <threadCount>4</threadCount>
           <perCoreThreadCount>true</perCoreThreadCount>
       </configuration>
   </plugin>
   ```

3. **Debug Test Failures**: Add the following to the CI pipeline to get more detailed logs:
   ```yaml
   - name: Run Integration Tests with Debug
     run: ./mvnw test -Dtest="com.zbib.hiresync.integration.**.*Test" -Dspring.profiles.active=test -X
   ```

## Implementation Approach
1. Apply database connection fix first in the pipeline.yaml
2. Create the wait-for-db.sh script
3. Test these changes to see if they resolve the issue
4. If still having problems, implement the environment configuration
5. Review and update test classes only if previous steps don't resolve the issue

## Success Criteria
- All integration tests pass successfully in the CI pipeline
- Pipeline execution completes in a reasonable time
- Test configuration follows Spring Boot best practices