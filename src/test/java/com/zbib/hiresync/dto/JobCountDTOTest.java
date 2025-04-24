package com.zbib.hiresync.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JobCountDTOTest {

    @Test
    void testJobCountDTO() {
        // Generate test UUIDs
        UUID jobId1 = UUID.randomUUID();
        UUID jobId2 = UUID.randomUUID();
        
        // Create DTO using constructor
        JobCountDTO dto1 = new JobCountDTO(jobId1, 5L);
        
        assertEquals(jobId1, dto1.getJobId());
        assertEquals(5L, dto1.getCount());
        
        // Create DTO using builder
        JobCountDTO dto2 = JobCountDTO.builder()
                .jobId(jobId2)
                .count(10L)
                .build();
        
        assertEquals(jobId2, dto2.getJobId());
        assertEquals(10L, dto2.getCount());
        
        // Test setters
        dto1.setJobId(jobId2);
        dto1.setCount(15L);
        
        assertEquals(jobId2, dto1.getJobId());
        assertEquals(15L, dto1.getCount());
        
        // Test equals and hashCode
        JobCountDTO dto3 = new JobCountDTO(jobId2, 10L);
        
        assertEquals(dto2, dto3);
        assertEquals(dto2.hashCode(), dto3.hashCode());
        
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }
} 