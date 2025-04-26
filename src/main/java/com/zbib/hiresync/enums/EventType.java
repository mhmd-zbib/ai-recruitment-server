package com.zbib.hiresync.enums;

/**
 * Types of events that can be processed asynchronously in the system
 */
public enum EventType {
    APPLICATION_RECEIVED("application.received"),
    CV_PARSING_STARTED("cv.parsing.started"),
    CV_PARSING_COMPLETED("cv.parsing.completed"),
    CV_PARSING_FAILED("cv.parsing.failed"),
    APPLICATION_RATED("application.rated");

    private final String eventName;

    EventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}