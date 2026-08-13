package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Human-readable smart insight derived from click patterns.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightItem {
    private String type; // e.g. "trend", "device", "location", "source"
    private String message;
}
