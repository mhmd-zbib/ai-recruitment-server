package com.zbib.hiresync.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Component
@Log4j2
public class RequestLogger {

    private final SensitiveDataMasker dataMasker;
    
    public RequestLogger(SensitiveDataMasker dataMasker) {
        this.dataMasker = dataMasker;
    }
    
    public HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    public void logRequest(String correlationId, String controllerName, String methodName, Object[] args) {
        Object[] maskedArgs = dataMasker.maskSensitiveData(args);
        HttpServletRequest request = getCurrentRequest();
        
        if (request != null) {
            log.info("[{}] -> {}:{} - {}", correlationId, request.getMethod(), request.getRequestURI(),
                    Arrays.toString(maskedArgs));
        } else {
            log.info("[{}] -> {}.{} - {}", correlationId, controllerName, methodName, Arrays.toString(maskedArgs));
        }
    }
}