package com.shortlink.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementation of AnalyticsService with idempotent event persistence and click analytics querying.
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UrlClickRepository urlClickRepository;

    @Override
    @Transactional
    public void processClickEvent(UrlClickedEvent event) {
        // Check for duplicate event processing (idempotency guard)
        if (urlClickRepository.existsByEventId(event.eventId())) {
            log.info("URL click event already processed: eventId={}", event.eventId());
            return;
        }

        try {
            UrlClick click = UrlClick.builder()
                    .eventId(event.eventId())
                    .shortCode(event.shortCode())
                    .clickedAt(event.timestamp())
                    .ipAddress(event.ipAddress())
                    .userAgent(event.userAgent())
                    .referrer(event.referrer())
                    .build();

            urlClickRepository.save(click);
            log.info("Processed URL click event: eventId={}, shortCode={}", event.eventId(), event.shortCode());
        } catch (DataIntegrityViolationException e) {
            // Protect against concurrent duplicate delivery race conditions at DB level
            log.info("URL click event already processed: eventId={}", event.eventId());
        } catch (Exception e) {
            log.error("Failed to process URL click event: eventId={}, shortCode={}", event.eventId(), event.shortCode(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalClicks(String shortCode) {
        return urlClickRepository.countByShortCode(shortCode);
    }
}
