package com.zbib.hiresync.enums;

/**
 * Types of work or project experiences in a CV
 */
public enum ExperienceType {
    FULL_TIME("Full-time"),
    PART_TIME("Part-time"),
    CONTRACT("Contract"),
    FREELANCE("Freelance"),
    INTERNSHIP("Internship"),
    PROJECT("Project"),
    VOLUNTEER("Volunteer");

    private final String displayName;

    ExperienceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmployment() {
        return this == FULL_TIME || this == PART_TIME || this == CONTRACT;
    }
    
    public boolean isProject() {
        return this == PROJECT;
    }
    
    public static ExperienceType fromString(String value) {
        for (ExperienceType type : ExperienceType.values()) {
            if (type.name().equalsIgnoreCase(value) || 
                type.getDisplayName().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ExperienceType: " + value);
    }
}