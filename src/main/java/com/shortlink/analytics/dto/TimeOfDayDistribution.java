package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Distribution of clicks by hour of day (00 to 23).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeOfDayDistribution {
    private String hour;
    private long count;
    private double percentage;
}
