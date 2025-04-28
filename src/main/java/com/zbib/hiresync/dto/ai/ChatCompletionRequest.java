package com.zbib.hiresync.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request object for chat completions API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {
    /**
     * The model to use for completion.
     */
    private String model;
    
    /**
     * The messages to generate completions for.
     */
    private List<ChatMessage> messages;
    
    /**
     * Controls randomness: 0 means deterministic, 1 means maximum randomness.
     */
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * Maximum number of tokens to generate.
     */
    @Builder.Default
    private Integer max_tokens = 1000;
    
    /**
     * Controls diversity via nucleus sampling.
     */
    @Builder.Default
    private Double top_p = 1.0;
    
    /**
     * Whether to stream the response.
     */
    @Builder.Default
    private Boolean stream = false;
}
