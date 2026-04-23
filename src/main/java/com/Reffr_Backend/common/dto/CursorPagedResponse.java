package com.Reffr_Backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPagedResponse<T> implements Serializable {
    private List<T> data;
    private Instant nextCursor;
    private boolean hasNext;
}
