package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.Application;
import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for managing skills
 */
@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional
    public void addSkillsToJobPost(JobPost jobPost, Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) return;
        Set<Skill> skills = new HashSet<>();
        for (String name : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(name);
                        return skillRepository.save(newSkill);
                    });

            jobPost.addSkill(skill);
            skills.add(skill);
        }
    }

    @Transactional
    public Skill createSkill(String name) {
        if (skillRepository.findByNameIgnoreCase(name).isPresent())
            throw new IllegalArgumentException("Skill already exists with name: " + name);

        Skill skill = new Skill();
        skill.setName(name);
        return skillRepository.save(skill);
    }
} 