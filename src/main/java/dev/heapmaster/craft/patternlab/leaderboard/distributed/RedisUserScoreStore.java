package dev.heapmaster.craft.patternlab.leaderboard.distributed;

import dev.heapmaster.craft.patternlab.leaderboard.UserScoreObserver;
import dev.heapmaster.craft.patternlab.leaderboard.UserScoreStore;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RedisUserScoreStore implements UserScoreStore, AutoCloseable {

  private static final String NAMESPACE = "leaderboard:";

  private final Map<String, Integer> l1Cache;
  private final List<UserScoreObserver> observers;
  private final RedisClient redisClient;
  private final StatefulRedisConnection<String, String> connection;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public RedisUserScoreStore(List<UserScoreObserver> observers) {
    this.l1Cache = new ConcurrentHashMap<>();
    RedisURI uri = RedisURI.Builder.redis("localhost", 6379).build();
    this.redisClient = RedisClient.create(uri);
    this.connection = redisClient.connect();

    this.observers = observers;
    scheduler.scheduleAtFixedRate(this::flushToRedis, 5, 5, TimeUnit.SECONDS);
  }

  @Override
  public void incrementScore(String userId, int delta) {
    l1Cache.compute(userId, (id, score) -> {
      var oldScore = score == null ? 0 : score;
      return oldScore + delta;
    });
    CompletableFuture.runAsync(() -> observers.forEach(observer -> observer.update(userId, delta)));
  }

  private synchronized void flushToRedis() {
    var syncCommands = connection.sync();
    l1Cache.forEach((userId, score) -> {
      syncCommands.incrby(NAMESPACE + userId, score);
    });
    l1Cache.clear();
  }

  @Override
  public void close() {
    redisClient.close();
    scheduler.shutdown();
  }
}
