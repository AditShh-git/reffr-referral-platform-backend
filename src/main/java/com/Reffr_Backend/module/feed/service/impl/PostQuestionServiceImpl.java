package com.Reffr_Backend.module.feed.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.config.AppProperties;
import com.Reffr_Backend.module.feed.dto.PostQuestionDto;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostQuestion;
import com.Reffr_Backend.module.feed.repository.PostQuestionRepository;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.feed.service.PostQuestionService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostQuestionServiceImpl implements PostQuestionService {

    private static final int MAX_QUESTIONS_PER_USER_PER_POST = 3;

    private final PostQuestionRepository questionRepository;
    private final PostRepository          postRepository;
    private final UserRepository          userRepository;
    private final StringRedisTemplate     redisTemplate;

    @Override
    @Transactional
    public PostQuestionDto.Response ask(UUID postId, UUID askerId, PostQuestionDto.AskRequest request) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        // Cannot ask on own post
        if (post.isOwnedBy(askerId)) {
            throw new BusinessException(ErrorCodes.CANNOT_ASK_OWN_POST, "You cannot ask questions on your own post");
        }

        // Redis-based anti-spam: max N questions per user per post per day
        String rateLimitKey = "pq_limit:" + askerId + ":" + postId + ":" + LocalDate.now();
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count == 1) {
            // First question today — set TTL to end of day
            redisTemplate.expire(rateLimitKey, Duration.ofDays(1));
        }
        if (count > MAX_QUESTIONS_PER_USER_PER_POST) {
            throw new BusinessException(ErrorCodes.RATE_LIMIT_EXCEEDED,
                    "You have reached the maximum of " + MAX_QUESTIONS_PER_USER_PER_POST +
                    " questions per post per day");
        }

        User asker = userRepository.findById(askerId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        PostQuestion question = PostQuestion.builder()
                .post(post)
                .asker(asker)
                .questionText(SanitizerUtils.sanitize(request.getQuestionText()))
                .build();

        PostQuestion saved = questionRepository.save(question);
        log.info("Question asked — postId={} asker={}", postId, askerId);
        return PostQuestionDto.Response.from(saved);
    }

    @Override
    @Transactional
    public PostQuestionDto.Response answer(UUID questionId, UUID authorId, PostQuestionDto.AnswerRequest request) {
        PostQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_QUESTION_NOT_FOUND, "Question not found"));

        // Only the post author can answer
        if (!question.getPost().isOwnedBy(authorId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the post author can answer questions");
        }
        if (question.isAnswered()) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "This question has already been answered");
        }

        question.answerWith(SanitizerUtils.sanitize(request.getAnswer()));
        log.info("Question answered — questionId={} author={}", questionId, authorId);
        return PostQuestionDto.Response.from(questionRepository.save(question));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostQuestionDto.Response> listByPost(UUID postId, Pageable pageable) {
        return questionRepository
                .findByPostIdAndVisibleTrueOrderByCreatedAtAsc(postId, pageable)
                .map(PostQuestionDto.Response::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostQuestionDto.Response> listUnanswered(UUID authorId, Pageable pageable) {
        return questionRepository
                .findUnansweredByPostAuthorId(authorId, pageable)
                .map(PostQuestionDto.Response::from);
    }
}
