package com.zbib.hiresync.enums;

public enum ApplicationStatus {
    NEW,           // Just submitted
    IN_REVIEW,     // Being assessed/interviewed
    SHORTLISTED,   // Passed initial screening, potential candidate
    OFFER,         // Offer stage (sent or negotiating)
    HIRED,         // Successfully hired
    REJECTED,      // Application rejected
    WITHDRAWN      // Candidate withdrew application
}
