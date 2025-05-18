package com.zbib.hiresync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbib.hiresync.dto.response.ApplicationFitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class ApplicationMatchService {

    private final RestTemplate restTemplate;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String url;

    public ApplicationFitResponse analyze(String jobDescription, String cvText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String prompt = String.format("""
                You are a hiring assistant.
                Analyze the job description and candidate resume.
                Respond with **ONLY** valid JSON — no markdown, no backticks, no extra text.
                The JSON must have these fields exactly:
                
                {
                  "interviewQuestions": [array of 3-5 technical or behavioral questions based on the job],
                  "matchRate": [integer from 0 to 100 indicating candidate fit percentage],
                  "summary": [short plain-text summary of candidate fit],
                }
                
                Job Description:
                %s
                
                Resume:
                %s
                """, jobDescription, cvText);


        Map<String, Object> requestBody = Map.of(
                "model", "meta-llama/llama-3.3-8b-instruct:free",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            String contentJson = new ObjectMapper()
                    .readTree(response.getBody())
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();
            log.info(contentJson);
            return new ObjectMapper().readValue(contentJson, ApplicationFitResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
