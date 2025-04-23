package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.Skill;
import com.zbib.hiresync.repository.projection.SkillCountProjection;
import org.springframework.data.jpa.domain.Specification;
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
    
    /**
     * Find top skills for job posts
     * 
     * @param limit the maximum number of skills to return
     * @return list of top skills with counts
     */
    @Query("SELECT s.name AS skillName, COUNT(DISTINCT j) AS count " +
           "FROM JobPost j " +
           "JOIN j.skills s " +
           "GROUP BY s.name " +
           "ORDER BY count DESC " +
           "LIMIT :limit")
    List<SkillCountProjection> findTopSkillsForJobPostSpec(@Param("limit") int limit);
} 