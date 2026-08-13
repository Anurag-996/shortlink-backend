package com.shortlink.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Time series point for the Admin Growth Chart (supporting Clicks, New Users, or New Links).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGrowthPoint {
    private String date;
    private long value;
}
