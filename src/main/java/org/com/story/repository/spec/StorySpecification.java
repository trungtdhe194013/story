package org.com.story.repository.spec;

import jakarta.persistence.criteria.*;
import org.com.story.entity.Category;
import org.com.story.entity.Chapter;
import org.com.story.entity.Story;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class StorySpecification {

    public static Specification<Story> isPublishedAndNotDeleted() {
        return (root, query, cb) -> {
            query.distinct(true);
            Predicate isApproved = cb.equal(root.get("status"), "APPROVED");
            Predicate isNotDeleted = cb.equal(root.get("isDeleted"), false);

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Chapter> chapterRoot = subquery.from(Chapter.class);
            subquery.select(chapterRoot.get("id"));
            subquery.where(
                    cb.equal(chapterRoot.get("story"), root),
                    cb.equal(chapterRoot.get("status"), "PUBLISHED")
            );

            return cb.and(isApproved, isNotDeleted, cb.exists(subquery));
        };
    }

    public static Specification<Story> hasStatus(String status) {
        return (root, query, cb) -> {
            boolean isCompleted = "COMPLETED".equalsIgnoreCase(status);
            return cb.equal(root.get("isCompleted"), isCompleted);
        };
    }

    public static Specification<Story> hasCategories(List<String> categories) {
        return (root, query, cb) -> {
            Join<Story, Category> categoryJoin = root.join("categories", JoinType.INNER);
            return categoryJoin.get("name").in(categories);
        };
    }

    public static Specification<Story> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), likePattern),
                    cb.like(cb.lower(root.get("summary")), likePattern)
            );
        };
    }

    public static Specification<Story> updatedInYear(Integer year) {
        return (root, query, cb) -> {
            LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
            LocalDateTime startOfNextYear = startOfYear.plusYears(1);
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("updatedAt"), startOfYear),
                    cb.lessThan(root.get("updatedAt"), startOfNextYear)
            );
        };
    }
}
