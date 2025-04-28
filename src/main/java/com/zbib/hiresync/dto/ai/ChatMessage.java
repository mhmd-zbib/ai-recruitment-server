package com.zbib.hiresync.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a message in a chat conversation with an AI model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    /**
     * The role of the message sender (system, user, assistant).
     */
    private String role;
    
    /**
     * The content of the message.
     */
    private String content;
}
