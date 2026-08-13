package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Distribution of clicks by day of week (Monday through Sunday).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayOfWeekDistribution {
    private String day;
    private long count;
    private double percentage;
}
