package com.zbib.hiresync.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

@Service
@RequiredArgsConstructor
public class PdfParsingService {

    public String parse(String pdfUrl) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            URI uri = new URI(pdfUrl);
            byte[] pdfBytes = restTemplate.getForObject(uri, byte[].class);

            try (PDDocument document = PDDocument.load(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download or parse PDF", e);
        }
    }
}
