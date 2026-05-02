package com.Reffr_Backend.module.feed.repository;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PostSpecification {

    public static Specification<Post> filterBy(
            Post.PostType type,
            String company,
            String role,
            Integer minExp,
            Integer maxExp,
            java.util.UUID authorId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by active, OPEN status, and not expired
            predicates.add(cb.isTrue(root.get("active")));
            predicates.add(cb.equal(root.get("status"), PostStatus.OPEN));
            
            predicates.add(cb.or(
                    cb.isNull(root.get("expiresAt")),
                    cb.greaterThan(root.get("expiresAt"), Instant.now())
            ));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }

            if (StringUtils.hasText(company)) {
                predicates.add(cb.like(cb.lower(root.get("company")), "%" + company.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(role)) {
                predicates.add(cb.like(cb.lower(root.get("currentRole")), "%" + role.toLowerCase() + "%"));
            }

            if (minExp != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("minExperience"), minExp));
            }

            if (maxExp != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxExperience"), maxExp));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
