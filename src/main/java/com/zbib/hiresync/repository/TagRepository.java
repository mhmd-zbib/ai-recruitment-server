package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.JobPost;
import com.zbib.hiresync.entity.Tag;
import com.zbib.hiresync.repository.projection.TagCountProjection;
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
    @Query("SELECT t.name AS tagName, COUNT(DISTINCT j) AS count " +
           "FROM JobPost j " +
           "JOIN j.tags t " +
           "GROUP BY t.name " +
           "ORDER BY count DESC " +
           "LIMIT :limit")
    List<TagCountProjection> findTopTagsForJobPostSpec(@Param("limit") int limit);
} 