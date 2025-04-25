package com.zbib.hiresync.repository;

import com.zbib.hiresync.dto.TagCountDTO;
import com.zbib.hiresync.entity.Job;
import com.zbib.hiresync.entity.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByNameIgnoreCase(String name);
    
    /**
     * Find top tags for job posts
     * 
     * @param limit the maximum number of tags to return
     * @return list of top tags with counts
     */
    @Query("SELECT new com.zbib.hiresync.dto.TagCountDTO(t.name, COUNT(DISTINCT j)) " +
           "FROM Job j " +
           "JOIN j.tags t " +
           "GROUP BY t.name " +
           "ORDER BY COUNT(DISTINCT j) DESC " +
           "LIMIT :limit")
    List<TagCountDTO> findTopTagsForJobPostSpec(@Param("limit") int limit);
} 