package com.zbib.hiresync.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

public class TagException extends AppException {

    private TagException(HttpStatus status, String userMessage, String logMessage) {
        super(status, userMessage, logMessage);
    }

    public static TagException notFound(UUID tagId) {
        return new TagException(NOT_FOUND,
                "Tag not found",
                formatLogMessage("Tag not found with ID", tagId.toString()));
    }

    public static TagException notFound(String tagName) {
        return new TagException(NOT_FOUND,
                "Tag not found",
                formatLogMessage("Tag not found with name", tagName));
    }

    public static TagException alreadyExists(String tagName) {
        return new TagException(CONFLICT,
                "Tag already exists",
                formatLogMessage("Attempted to create a tag that already exists", tagName));
    }

    public static TagException inUse(UUID tagId) {
        return new TagException(CONFLICT,
                "Cannot delete tag that is in use",
                formatLogMessage("Attempted to delete a tag that is in use, tag ID", tagId.toString()));
    }

    private static String formatLogMessage(String message, String value) {
        return String.format("%s: [%s]", message, value);
    }
}