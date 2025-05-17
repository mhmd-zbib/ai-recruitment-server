package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing skill data
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    
    /**
     * Find a skill by its name (case insensitive)
     *
     * @param name skill name
     * @return optional containing the skill if found
     */
    Optional<Skill> findByNameIgnoreCase(String name);
}