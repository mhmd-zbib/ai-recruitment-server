#!/usr/bin/env bash

# Description: Creates new code components like controllers, services, entities, etc. from templates.

# Exit on error
set -e

# Get the project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Source common utilities
source "$SCRIPT_DIR/logging.sh"

# Container name
DEVTOOLS_CONTAINER="hiresync-devtools"

# Default values
TEMPLATE_DIR="$PROJECT_ROOT/scripts/templates"
COMPONENT_TYPE=""
NAME=""
PACKAGE=""
BASE_PACKAGE="com.hiresync"

# Show usage
usage() {
  echo "Usage: ./hiresync generate COMPONENT_TYPE NAME [OPTIONS]"
  echo ""
  echo "Generate new code components from templates"
  echo ""
  echo "Component Types:"
  echo "  controller      Create a new REST controller"
  echo "  service         Create a new service"
  echo "  repository      Create a new repository interface"
  echo "  entity          Create a new JPA entity"
  echo "  dto             Create a new DTO class"
  echo "  exception       Create a new custom exception"
  echo "  test            Create a new test class"
  echo ""
  echo "Options:"
  echo "  --package=NAME  Target package name (default: derived from component type)"
  echo "  --help          Show this help message"
  echo ""
  echo "Examples:"
  echo "  ./hiresync generate controller User"
  echo "  ./hiresync generate service EmailNotification --package=com.hiresync.notification"
  exit 0
}

# Parse arguments
if [ "$1" == "--help" ]; then
  usage
fi

# Get component type and name
COMPONENT_TYPE="$1"
NAME="$2"
shift 2

# Check if required arguments are provided
if [ -z "$COMPONENT_TYPE" ] || [ -z "$NAME" ]; then
  log_error "Missing required arguments"
  usage
fi

# Parse remaining options
while [[ $# -gt 0 ]]; do
  case "$1" in
    --package=*)
      PACKAGE="${1#*=}"
      shift
      ;;
    *)
      log_error "Unknown option: $1"
      usage
      ;;
  esac
done

# Determine target package based on component type if not specified
if [ -z "$PACKAGE" ]; then
  case "$COMPONENT_TYPE" in
    controller)
      PACKAGE="$BASE_PACKAGE.controller"
      ;;
    service)
      PACKAGE="$BASE_PACKAGE.service"
      ;;
    repository)
      PACKAGE="$BASE_PACKAGE.repository"
      ;;
    entity)
      PACKAGE="$BASE_PACKAGE.model.entity"
      ;;
    dto)
      PACKAGE="$BASE_PACKAGE.model.dto"
      ;;
    exception)
      PACKAGE="$BASE_PACKAGE.exception"
      ;;
    test)
      PACKAGE="$BASE_PACKAGE.test"
      ;;
    *)
      log_error "Unknown component type: $COMPONENT_TYPE"
      usage
      ;;
  esac
fi

log_section "Generating $COMPONENT_TYPE: $NAME"
log_info "Target package: $PACKAGE"

# Create target directory
TARGET_DIR="$PROJECT_ROOT/src/main/java/$(echo $PACKAGE | tr '.' '/')"
mkdir -p "$TARGET_DIR"

# For tests, create in test directory instead
if [ "$COMPONENT_TYPE" == "test" ]; then
  TARGET_DIR="$PROJECT_ROOT/src/test/java/$(echo $PACKAGE | tr '.' '/')"
  mkdir -p "$TARGET_DIR"
fi

# Check if container is running (for Freemarker template processing)
if docker ps --format '{{.Names}}' | grep -q "$DEVTOOLS_CONTAINER"; then
  log_info "Using container for code generation"
  # Execute template processing inside container
  docker exec -it "$DEVTOOLS_CONTAINER" bash -c "cd /workspace && java -cp target/classes com.hiresync.util.CodeGenerator $COMPONENT_TYPE $NAME $PACKAGE"
else
  # Simple template-based generation
  log_info "Using basic template generation"
  
  case "$COMPONENT_TYPE" in
    controller)
      cat > "$TARGET_DIR/${NAME}Controller.java" << EOF
package $PACKAGE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/${NAME,lower}")
@RequiredArgsConstructor
public class ${NAME}Controller {
    
    // TODO: Add service dependencies
    
    @GetMapping
    public ResponseEntity<?> getAll() {
        // TODO: Implement method
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        // TODO: Implement method
        return ResponseEntity.ok().build();
    }
    
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Object request) {
        // TODO: Implement method
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Object request) {
        // TODO: Implement method
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // TODO: Implement method
        return ResponseEntity.ok().build();
    }
}
EOF
      ;;
    
    service)
      cat > "$TARGET_DIR/${NAME}Service.java" << EOF
package $PACKAGE;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ${NAME}Service {
    
    // TODO: Add repository dependencies
    
    @Transactional(readOnly = true)
    public void findAll() {
        // TODO: Implement method
    }
    
    @Transactional(readOnly = true)
    public void findById(Long id) {
        // TODO: Implement method
    }
    
    @Transactional
    public void create(Object request) {
        // TODO: Implement method
    }
    
    @Transactional
    public void update(Long id, Object request) {
        // TODO: Implement method
    }
    
    @Transactional
    public void delete(Long id) {
        // TODO: Implement method
    }
}
EOF
      ;;
    
    repository)
      cat > "$TARGET_DIR/${NAME}Repository.java" << EOF
package $PACKAGE;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ${NAME}Repository extends JpaRepository<Object, Long> {
    // TODO: Add custom query methods
}
EOF
      ;;
    
    entity)
      cat > "$TARGET_DIR/${NAME}.java" << EOF
package $PACKAGE;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "${NAME,lower}s")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ${NAME} {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // TODO: Add entity fields
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
EOF
      ;;
    
    dto)
      cat > "$TARGET_DIR/${NAME}Dto.java" << EOF
package $PACKAGE;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${NAME}Dto {
    
    private Long id;
    
    // TODO: Add DTO fields
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
EOF
      ;;
    
    exception)
      cat > "$TARGET_DIR/${NAME}Exception.java" << EOF
package $PACKAGE;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ${NAME}Exception extends RuntimeException {
    
    public ${NAME}Exception(String message) {
        super(message);
    }
    
    public ${NAME}Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
EOF
      ;;
    
    test)
      cat > "$TARGET_DIR/${NAME}Test.java" << EOF
package $PACKAGE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ${NAME}Test {
    
    // TODO: Add dependencies
    
    @BeforeEach
    void setUp() {
        // TODO: Setup test environment
    }
    
    @Test
    @DisplayName("Test should pass")
    void testShouldPass() {
        // TODO: Implement test
        assertTrue(true);
    }
}
EOF
      ;;
  esac
fi

log_success "${COMPONENT_TYPE^} ${NAME} created successfully"
log_info "File: ${TARGET_DIR}/${NAME}${COMPONENT_TYPE^}.java" 