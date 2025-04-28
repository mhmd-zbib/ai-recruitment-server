package com.zbib.hiresync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AIChatController.class)
class AIChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIChatService aiChatService;

    private AIChatRequest chatRequest;
    private AIChatResponse chatResponse;

    @BeforeEach
    void setUp() {
        // Set up the chat request
        chatRequest = AIChatRequest.builder()
                .message("Hello, how can you help me with recruitment?")
                .build();

        // Set up the chat response
        AIChatResponse.Usage usage = AIChatResponse.Usage.builder()
                .promptTokens(50)
                .completionTokens(100)
                .totalTokens(150)
                .build();

        chatResponse = AIChatResponse.builder()
                .message("I can help you with various recruitment tasks such as screening candidates, writing job descriptions, and conducting interviews.")
                .model("anthropic/claude-3-opus")
                .usage(usage)
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void chat_Success() throws Exception {
        // Arrange
        when(aiChatService.chat(any(AIChatRequest.class))).thenReturn(chatResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(chatResponse.getMessage()))
                .andExpect(jsonPath("$.model").value(chatResponse.getModel()))
                .andExpect(jsonPath("$.usage.promptTokens").value(chatResponse.getUsage().getPromptTokens()))
                .andExpect(jsonPath("$.usage.completionTokens").value(chatResponse.getUsage().getCompletionTokens()))
                .andExpect(jsonPath("$.usage.totalTokens").value(chatResponse.getUsage().getTotalTokens()));
    }

    @Test
    void chat_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(post("/v1/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void chat_InvalidRequest() throws Exception {
        // Arrange - Empty message
        chatRequest.setMessage("");

        // Act & Assert
        mockMvc.perform(post("/v1/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isBadRequest());
    }
}
