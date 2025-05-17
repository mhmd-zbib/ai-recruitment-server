package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.exception.TagException;
import com.zbib.hiresync.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public Set<Tag> getOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Tag> tags = new HashSet<>();
        
        for (String name : tagNames) {
            String normalizedName = name.trim();
            
            if (normalizedName.isEmpty()) {
                continue;
            }
                        Tag tag = tagRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name(normalizedName)
                                .build();
                        return tagRepository.save(newTag);
                    });
            
            tags.add(tag);
        }
        return tags;
    }
}