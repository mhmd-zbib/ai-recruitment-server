package com.zbib.hiresync.dto.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortCriteria {
    private String field;
    private Sort.Direction direction = Sort.Direction.ASC;
    
    public static SortCriteria asc(String field) {
        return new SortCriteria(field, Sort.Direction.ASC);
    }
    
    public static SortCriteria desc(String field) {
        return new SortCriteria(field, Sort.Direction.DESC);
    }
} 