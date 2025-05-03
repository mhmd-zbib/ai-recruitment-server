# CI Pipeline Optimization Plan

## Overview
This document outlines the issues identified in the existing CI pipeline workflow (`pipeline.yaml`) and provides a detailed action plan to fix and optimize the workflow for the HireSync AI Recruitment Server application.

## Project Context
- **Application Type**: Spring Boot REST API (Java 21)
- **Build Tool**: Maven
- **Database**: PostgreSQL 16
- **Containerization**: Docker
- **Main Artifact**: JAR file named `hiresync-1.0.0.jar`
- **Health Check Endpoint**: `/api/actuator/health`
- **Docker Setup**: Multi-stage environment with test and production configurations

## Problems Identified

### 1. Job Dependency Issues
- **Setup Job**: Not properly sharing state with downstream jobs, causing redundant downloads and setup
- **Lint Job**: Contains contradictory configurations (`checkstyle:check` with `-Dcheckstyle.skip=true`)
- **Test Execution Order**: Unit tests run after build job completes, creating an inefficient critical path
- **Integration Test Environment**: Connection settings mismatch between pipeline and Docker Compose configuration
  - Pipeline: `SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb`
  - Docker Compose: Container name is `postgres` and should be used as hostname
- **Docker Build Dependencies**: Building occurs after all tests, preventing parallel execution
- **Deployment Security**: Missing branch/tag conditions for deployment to Docker Hub

### 2. Technical Implementation Issues
- **JAR File Naming**: Hardcoded as `hiresync-1.0.0.jar` but Dockerfile uses wildcard pattern `hiresync-*.jar`
- **Code Checkout Redundancy**: Each job checks out code separately
- **Docker Image Build & Test**: Sequential execution instead of parallel when possible
- **Environment Variables**: Not consistent between pipeline and docker-compose.test.yaml
- **Maven Wrapper Permissions**: Requires explicit `chmod +x ./mvnw` in setup job

## Action Plan

### 1. Setup Job Improvements
- [ ] **Check**: Verify Maven wrapper is executable in repository
  - Command: `git ls-files --stage mvnw` to confirm executable bit is set
- [ ] **Check**: Ensure Java 21 (Temurin) is specified in pom.xml
  - Look for `<java.version>21</java.version>` in properties
- [ ] **Fix**: Configure proper caching for Maven dependencies
  - Current: Basic cache setting `cache: 'maven'`
  - Improved: Add `.m2/repository` path with restore keys
- [ ] **Fix**: Create workspace artifact to share project state between jobs
  - Add upload step for workspace files excluding large directories

### 2. Lint Job Improvements
- [ ] **Check**: Verify the checkstyle configuration in pom.xml
  - Look for `maven-checkstyle-plugin` configuration
- [ ] **Check**: Validate the `skip.checks` property behavior in Maven profiles
  - Examine if `-Dcheckstyle.skip=true` conflicts with intended behavior
- [ ] **Fix**: Remove contradictory `-Dcheckstyle.skip=true` flag
  - Current: `./mvnw checkstyle:check -Dcheckstyle.skip=true`
  - Fix: `./mvnw checkstyle:check`
- [ ] **Fix**: Use the proper Maven profile for linting
  - Add `-P lint` profile if defined, or create one in pom.xml

### 3. Build Job Optimizations
- [ ] **Check**: Confirm the correct artifact version in pom.xml
  - Verify `<version>1.0.0</version>` in pom.xml
- [ ] **Check**: Verify build command and required parameters
  - Current: `./mvnw clean package -DskipTests=true`
  - Validate if other flags are needed (`-B`, `-ntp`, etc.)
- [ ] **Fix**: Use wildcard for JAR path when uploading artifacts
  - Current: `path: target/hiresync-1.0.0.jar`
  - Fix: `path: target/hiresync-*.jar`
- [ ] **Fix**: Consider running in parallel with unit tests
  - Restructure dependencies to allow tests to run in parallel with build

### 4. Test Job Reorganization
- [ ] **Check**: Verify test execution patterns
  - Current pattern: `-Dtest="com.zbib.hiresync.unit.**.*Test"`
  - Validate against actual test class organization
- [ ] **Check**: Validate test dependencies and environment requirements
  - Unit tests may not need database connection
- [ ] **Fix**: Move unit tests to run before or parallel with build
  - Unit tests can run immediately after lint
- [ ] **Fix**: Configure proper test isolation and reporting
  - Ensure integration and unit tests are properly separated

### 5. Docker Build Improvements
- [ ] **Check**: Verify Dockerfile correctness
  - Base image: `eclipse-temurin:21-jre`
  - Entry point: `java $JAVA_OPTS -jar /app/app.jar`
  - Health check dependencies: curl is installed
- [ ] **Check**: Validate Docker image naming and tagging strategy
  - Current: `hiresync:test` → `${{ secrets.DOCKERHUB_USERNAME }}/hiresync:latest`
- [ ] **Fix**: Optimize dependencies to allow parallel execution
  - Docker build can start after successful build job without waiting for all tests
- [ ] **Fix**: Improve caching for Docker build layers
  - Use BuildX cache more effectively with cache-from/cache-to options

### 6. Integration Test Fixes
- [ ] **Check**: Verify docker-compose.test.yaml configuration
  - Services: `postgres` (DB) and `app` (application)
  - Network: `app-network` bridge
- [ ] **Check**: Validate integration test execution patterns
  - Current: `-Dtest="com.zbib.hiresync.integration.**.*Test"`
- [ ] **Fix**: Update database connection parameters to use container networking
  - Current: `SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb`
  - Fix: `SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/testdb`
- [ ] **Fix**: Improve service health checking and readiness
  - Add proper wait for PostgreSQL health check
  - Current: Simple sleep 15s delay
  - Fix: Use polling mechanism with retry

### 7. Deployment Security Enhancement
- [ ] **Check**: Verify Docker Hub authentication process
  - Using `docker/login-action@v3` with secrets
- [ ] **Check**: Validate image tagging and versioning strategy
  - Current tags: latest and ${{ github.sha }}
- [ ] **Fix**: Add conditional execution based on branch/tag
  - Only deploy on `master` branch or tags
  - Add: `if: github.ref == 'refs/heads/master' || startsWith(github.ref, 'refs/tags/')`
- [ ] **Fix**: Implement proper secrets management
  - Ensure secrets are only used in deployment job

### 8. Overall Workflow Optimization
- [ ] **Check**: Analyze total workflow execution time
  - Identify bottlenecks based on GitHub Actions run history
- [ ] **Check**: Verify all required actions are properly defined
  - Review all steps for proper naming and error handling
- [ ] **Fix**: Reduce redundant operations (code checkout, setup)
  - Use workspace artifacts instead of multiple checkouts
- [ ] **Fix**: Implement parallel execution where possible
  - Reorganize job dependencies to maximize parallelism

## Implementation Approach
1. Make incremental changes to pipeline.yaml, testing each modification
2. Focus on critical path optimizations first (build & test parallelization)
3. Document all changes for future reference
4. Set up monitoring for pipeline performance metrics

## Success Metrics
- Reduced overall pipeline execution time (target: 25% reduction)
- Improved reliability (fewer failed builds due to environment issues)
- Simplified configuration with less redundancy
- Clear dependency chain between jobs
- Proper control flow for deployment to Docker Hub

## Application Specific Considerations
- Spring Boot application with health check endpoint at `/api/actuator/health`
- PostgreSQL database connections need proper network configuration
- JWT configuration must be properly passed through environment variables
- Application uses different Spring profiles (local, test, prod) that affect behavior
- Integration tests require a functioning database instance