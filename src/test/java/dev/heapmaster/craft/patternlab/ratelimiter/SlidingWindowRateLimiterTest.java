package dev.heapmaster.craft.patternlab.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlidingWindowRateLimiterTest {

  private static final int MAX_REQUESTS = 5;
  private static final int WINDOW_SIZE_SECONDS = 10;
  private RateLimiter rateLimiter;
  private Clock clock;

  @BeforeEach
  void init() {
    clock = new MutableClock(Instant.now());
    rateLimiter = new SlidingWindowRateLimiter(MAX_REQUESTS, WINDOW_SIZE_SECONDS, clock);
  }

  @Test
  void testAllowRequestsWithinLimit() {
    String userId = "user";
    for (int i = 0; i < MAX_REQUESTS; i++) {
      assertTrue(rateLimiter.allowRequest(userId));
    }
    assertFalse(rateLimiter.allowRequest(userId));

    ((MutableClock)clock).advanceSeconds(WINDOW_SIZE_SECONDS + 1);

    for (int i = 0; i < MAX_REQUESTS; i++) {
      assertTrue(rateLimiter.allowRequest(userId));
    }
  }
}
