package dev.heapmaster.craft.patternlab.ratelimiter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InMemorySlidingWindowRateLimiter implements RateLimiter, AutoCloseable {

  private static final int CLEANUP_INTERVAL_SECONDS = 60;
  private final int maxRequests;
  private final int windowSizeInSeconds;
  private final Map<String, Queue<Instant>> userRequestTimestamps;
  private final Map<String, Object> locks;
  private final Clock clock;
  private final ScheduledExecutorService scheduler;

  public InMemorySlidingWindowRateLimiter(int maxRequests, int windowSizeInSeconds, Clock clock) {
    this.maxRequests = maxRequests;
    this.windowSizeInSeconds = windowSizeInSeconds;
    this.userRequestTimestamps = new ConcurrentHashMap<>();
    this.locks = new ConcurrentHashMap<>();
    this.clock = clock;
    this.scheduler = Executors.newScheduledThreadPool(1);

    this.scheduler.scheduleAtFixedRate(this::cleanUpIdle, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private void cleanUpIdle() {
    var now = clock.instant();
    List<String> userIds = new ArrayList<>(userRequestTimestamps.keySet());

    for (var userId : userIds) {
      var lock = locks.get(userId);
      if (lock != null) {
        synchronized (lock) {
          var requests = userRequestTimestamps.get(userId);
          cleanUpOldRequests(requests, now);
          if (requests.isEmpty()) {
            userRequestTimestamps.remove(userId);
            locks.remove(userId);
          }
        }
      }
    }
  }

  @Override
  public boolean allowRequest(String userId) {
    var now = clock.instant();
    var lock = locks.computeIfAbsent(userId, k -> new Object());

    synchronized (lock) {
      var queue = userRequestTimestamps.computeIfAbsent(userId, k -> new LinkedList<>());
      cleanUpOldRequests(queue, now);
      if (queue.size() < maxRequests) {
        queue.add(now);
        return true;
      }
      return false;
    }
  }

  private void cleanUpOldRequests(Queue<Instant> requests, Instant now) {
    var oldestInWindow = now.minusSeconds(windowSizeInSeconds);
    while (!requests.isEmpty()) {
      var timestamp = requests.peek();
      if (timestamp.isBefore(oldestInWindow)) {
        requests.poll();
      } else {
        break;
      }
    }
  }

  @Override
  public void close() {
    scheduler.shutdown();
  }
}
