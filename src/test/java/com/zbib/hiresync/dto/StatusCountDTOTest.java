package com.zbib.hiresync.dto;

import com.zbib.hiresync.enums.ApplicationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StatusCountDTOTest {

    @Test
    void testStatusCountDTO() {
        // Create DTO using constructor
        StatusCountDTO dto1 = new StatusCountDTO(ApplicationStatus.SUBMITTED, 5L);
        
        assertEquals(ApplicationStatus.SUBMITTED, dto1.getStatus());
        assertEquals(5L, dto1.getCount());
        
        // Create DTO using builder
        StatusCountDTO dto2 = StatusCountDTO.builder()
                .status(ApplicationStatus.UNDER_REVIEW)
                .count(10L)
                .build();
        
        assertEquals(ApplicationStatus.UNDER_REVIEW, dto2.getStatus());
        assertEquals(10L, dto2.getCount());
        
        // Test setters
        dto1.setStatus(ApplicationStatus.INTERVIEWED);
        dto1.setCount(15L);
        
        assertEquals(ApplicationStatus.INTERVIEWED, dto1.getStatus());
        assertEquals(15L, dto1.getCount());
        
        // Test equals and hashCode
        StatusCountDTO dto3 = new StatusCountDTO(ApplicationStatus.UNDER_REVIEW, 10L);
        
        assertEquals(dto2, dto3);
        assertEquals(dto2.hashCode(), dto3.hashCode());
        
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }
} 