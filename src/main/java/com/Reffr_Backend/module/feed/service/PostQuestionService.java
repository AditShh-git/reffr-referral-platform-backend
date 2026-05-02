package com.Reffr_Backend.module.feed.service;

import com.Reffr_Backend.module.feed.dto.PostQuestionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PostQuestionService {

    /** Ask a question on a post. Anti-spam: max 3 questions per user per post. */
    PostQuestionDto.Response ask(UUID postId, UUID askerId, PostQuestionDto.AskRequest request);

    /** Post author answers a specific question. */
    PostQuestionDto.Response answer(UUID questionId, UUID authorId, PostQuestionDto.AnswerRequest request);

    /** List visible questions for a post, chronological order. */
    Page<PostQuestionDto.Response> listByPost(UUID postId, Pageable pageable);

    /** Post author's unanswered questions queue (across all their posts). */
    Page<PostQuestionDto.Response> listUnanswered(UUID authorId, Pageable pageable);
}
