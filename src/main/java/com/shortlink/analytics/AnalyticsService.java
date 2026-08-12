package com.shortlink.analytics;

// Interface defining business logic for click analytics processing and querying.
public interface AnalyticsService {

    // Processes a consumed URL click event idempotently and persists it into PostgreSQL.
    void processClickEvent(UrlClickedEvent event);

    // Retrieves total click count for a given short code.
    long getTotalClicks(String shortCode);
}
