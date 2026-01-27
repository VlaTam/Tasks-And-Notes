package dev.heapmaster.craft.patternlab.ratelimiter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {

  private Instant currentTime;

  public MutableClock(Instant currentTime) {
    this.currentTime = currentTime;
  }

  public void advanceSeconds(long seconds) {
    currentTime = currentTime.plusSeconds(seconds);
  }

  @Override
  public ZoneId getZone() {
    return null;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return null;
  }

  @Override
  public Instant instant() {
    return currentTime;
  }
}
