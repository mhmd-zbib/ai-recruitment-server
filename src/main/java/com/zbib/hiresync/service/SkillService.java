package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional
    public void addSkillsToJob(Job job, Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) return;
        for (String name : skillNames) {
            Skill skill = skillRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Skill newSkill = new Skill();
                        newSkill.setName(name);
                        return skillRepository.save(newSkill);
                    });

            job.addSkill(skill);
        }
    }

    @Transactional(readOnly = true)
    public Skill findByNameIgnoreCase(String name) {
        return skillRepository.findByNameIgnoreCase(name).orElse(null);
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