package com.zbib.hiresync.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationFitResponse {
    private List<String> interviewQuestions;
    private int matchRate;
    private String summary;
}
