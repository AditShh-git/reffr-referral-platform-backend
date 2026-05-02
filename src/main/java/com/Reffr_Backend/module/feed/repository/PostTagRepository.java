package com.Reffr_Backend.module.feed.repository;

import com.Reffr_Backend.module.feed.entity.PostTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    @Query("""
        SELECT t.tag, COUNT(t)
        FROM PostTag t
        JOIN t.post p
        WHERE p.active = true
          AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
        GROUP BY t.tag
        ORDER BY COUNT(t) DESC
    """)
    List<Object[]> findTrendingSkills(Pageable pageable);
}
