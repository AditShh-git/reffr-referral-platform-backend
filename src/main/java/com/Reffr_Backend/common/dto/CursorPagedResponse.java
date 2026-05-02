package com.Reffr_Backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generic cursor-paginated response envelope.
 *
 * <p>JSON field names are deliberately explicit:
 * <pre>
 * {
 *   "success": true,
 *   "data": {
 *     "posts": [...],         ← @JsonProperty("posts") on items
 *     "nextCursor": "...",
 *     "hasNext": true
 *   }
 * }
 * </pre>
 *
 * The field is named {@code items} internally so the generic class stays
 * type-agnostic, but the serialised name is always {@code "posts"} — matching
 * the product API contract and avoiding the old {@code data.data} double-nest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPagedResponse<T> implements Serializable {

    /**
     * The page of results.
     * Serialised as {@code "posts"} in JSON regardless of the generic type {@code T}.
     */
    @JsonProperty("posts")
    private List<T> items;

    private Instant nextCursor;
    private UUID nextCursorId;
    private boolean hasNext;
}
