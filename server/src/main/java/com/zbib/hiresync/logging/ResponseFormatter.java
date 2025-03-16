package com.zbib.hiresync.logging;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseFormatter {
    
    private final SensitiveDataMasker dataMasker;
    
    public ResponseFormatter(SensitiveDataMasker dataMasker) {
        this.dataMasker = dataMasker;
    }
    
    public String formatResponse(Object result) {
        if (result == null) {
            return "null";
        }

        if (result instanceof ResponseEntity<?> response) {
            int statusCode = response.getStatusCodeValue();
            Object body = dataMasker.maskSensitiveData(response.getBody());
            return statusCode + " " + body;
        }

        return String.valueOf(dataMasker.maskSensitiveData(result));
    }
}