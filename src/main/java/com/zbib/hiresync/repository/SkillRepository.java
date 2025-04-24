package com.zbib.hiresync.repository;

import com.zbib.hiresync.dto.SkillCountDTO;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByNameIgnoreCase(String name);
    
    List<Skill> findByCategoryIgnoreCase(String category);
    
    /**
     * Find top skills for job posts
     * 
     * @param limit the maximum number of skills to return
     * @return list of top skills with counts
     */
    @Query("SELECT new com.zbib.hiresync.dto.SkillCountDTO(s.name, COUNT(DISTINCT j)) " +
           "FROM Job j " +
           "JOIN j.skills s " +
           "GROUP BY s.name " +
           "ORDER BY COUNT(DISTINCT j) DESC " +
           "LIMIT :limit")
    List<SkillCountDTO> findTopSkillsForJobPostSpec(@Param("limit") int limit);
} 