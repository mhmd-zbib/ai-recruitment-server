package com.zbib.hiresync.service;

import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional
    public Set<Skill> getOrCreateSkills(Set<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Skill> skills = new HashSet<>();
        
        for (String name : skillNames) {
            String normalizedName = name.trim();
            
            if (normalizedName.isEmpty()) {
                continue;
            }
            
            Skill skill = skillRepository.findByNameIgnoreCase(normalizedName)
                    .orElseGet(() -> {
                        Skill newSkill = Skill.builder()
                                .name(normalizedName)
                                .build();
                        return skillRepository.save(newSkill);
                    });
            
            skills.add(skill);
        }
        return skills;
    }
}