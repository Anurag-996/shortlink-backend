package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Item in a categorical breakdown (devices, browsers, countries, referrers).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionItem {
    private String label;
    private long count;
    private double percentage;
}
