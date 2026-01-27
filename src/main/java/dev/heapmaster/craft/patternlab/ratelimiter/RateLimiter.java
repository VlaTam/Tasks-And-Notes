package dev.heapmaster.craft.patternlab.ratelimiter;

public interface RateLimiter {

  boolean allowRequest(String userId);
}
