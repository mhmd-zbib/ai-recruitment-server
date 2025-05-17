package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class SkillException extends AppException {

    private SkillException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static SkillException skillNotFound(UUID skillId) {
        return new SkillException(NOT_FOUND,
                "Skill not found",
                formatLogMessage("Skill not found with ID", skillId.toString()));
    }

    public static SkillException skillNotFound(String skillName) {
        return new SkillException(NOT_FOUND,
                "Skill not found",
                formatLogMessage("Skill not found with name", skillName));
    }

    public static SkillException alreadyExists(String skillName) {
        return new SkillException(CONFLICT,
                "Skill already exists",
                formatLogMessage("Attempted to create a skill that already exists", skillName));
    }

    public static SkillException inUse(UUID skillId) {
        return new SkillException(CONFLICT,
                "Cannot delete skill that is in use",
                formatLogMessage("Attempted to delete a skill that is in use, skill ID", skillId.toString()));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}