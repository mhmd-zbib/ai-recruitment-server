package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public void addTagsToJob(Job job, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        for (String name : tagNames) {
            Tag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(name);
                        return tagRepository.save(newTag);
                    });
            job.addTag(tag);
        }
    }

    @Transactional(readOnly = true)
    public Tag findByName(String name) {
        return tagRepository.findByNameIgnoreCase(name).orElse(null);
    }

    @Transactional
    public Tag createTag(String name) {
        if (tagRepository.findByNameIgnoreCase(name).isPresent())
            throw new IllegalArgumentException("Tag already exists with name: " + name);
        Tag tag = new Tag();
        tag.setName(name);
        return tagRepository.save(tag);
    }
}