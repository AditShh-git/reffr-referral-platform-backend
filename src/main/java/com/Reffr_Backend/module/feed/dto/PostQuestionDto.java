package com.Reffr_Backend.module.feed.dto;

import com.Reffr_Backend.module.feed.entity.PostQuestion;
import com.Reffr_Backend.module.user.dto.UserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

public class PostQuestionDto {

    @Getter
    @Setter
    public static class AskRequest {
        @NotBlank(message = "Question text is required")
        @Size(min = 5, max = 500, message = "Question must be between 5 and 500 characters")
        private String questionText;
    }

    @Getter
    @Setter
    public static class AnswerRequest {
        @NotBlank(message = "Answer text is required")
        @Size(max = 1000, message = "Answer must be 1000 characters or fewer")
        private String answer;
    }

    @Getter
    @Builder
    public static class Response {
        private UUID   id;
        private UUID   postId;
        private UserDto.UserSummary asker;
        private String questionText;
        private String answer;
        private boolean answered;
        private Instant answeredAt;
        private Instant createdAt;

        public static Response from(PostQuestion q) {
            return Response.builder()
                    .id(q.getId())
                    .postId(q.getPost().getId())
                    .asker(UserDto.UserSummary.from(q.getAsker()))
                    .questionText(q.getQuestionText())
                    .answer(q.getAnswer())
                    .answered(q.isAnswered())
                    .answeredAt(q.getAnsweredAt())
                    .createdAt(q.getCreatedAt())
                    .build();
        }
    }
}
