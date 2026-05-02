package com.Reffr_Backend.module.feed.repository;

import com.Reffr_Backend.module.feed.entity.PostQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PostQuestionRepository extends JpaRepository<PostQuestion, UUID> {

    /** Public questions for a post — visible, chronological. */
    @EntityGraph(attributePaths = {"asker"})
    Page<PostQuestion> findByPostIdAndVisibleTrueOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    /** Unanswered questions for a post's author to respond to. */
    @EntityGraph(attributePaths = {"asker", "post"})
    @Query("""
        SELECT q FROM PostQuestion q
        WHERE q.post.author.id = :authorId
          AND q.answer IS NULL
          AND q.visible = true
        ORDER BY q.createdAt ASC
    """)
    Page<PostQuestion> findUnansweredByPostAuthorId(@Param("authorId") UUID authorId, Pageable pageable);

    /** How many questions a specific user has asked on a specific post — for anti-spam. */
    long countByPostIdAndAskerId(UUID postId, UUID askerId);

    /** Fetch single question with author relationship. */
    @EntityGraph(attributePaths = {"asker", "post", "post.author"})
    Optional<PostQuestion> findById(UUID id);
}
