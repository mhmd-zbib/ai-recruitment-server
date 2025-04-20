package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing tags
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public void addTagsToJobPost(JobPost jobPost, Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            Tag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Tag newTag = new Tag();
                        newTag.setName(name);
                        return tagRepository.save(newTag);
                    });
            jobPost.addTag(tag);
            tags.add(tag);
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