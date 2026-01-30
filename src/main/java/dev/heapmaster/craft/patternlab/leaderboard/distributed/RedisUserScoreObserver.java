package dev.heapmaster.craft.patternlab.leaderboard.distributed;

import dev.heapmaster.craft.patternlab.leaderboard.UserScoreObserver;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.List;

public class RedisUserScoreObserver implements UserScoreObserver, AutoCloseable {

  private static final String SORTED_SET_KEY = "leaderboard:sortedset";

  private final RedisClient redisClient;
  private final StatefulRedisConnection<String, String> connection;

  public RedisUserScoreObserver() {
    this.redisClient = RedisClient.create("redis://localhost:6379");
    this.connection = redisClient.connect();
  }

  @Override
  public void update(String userId, int delta) {
    connection.sync().zincrby(SORTED_SET_KEY, delta, userId);
  }

  @Override
  public List<String> getTopN(int n) {
    var syncCommands = connection.sync();
    return syncCommands.zrevrange(SORTED_SET_KEY, 0, n - 1L);
  }

  @Override
  public void close() throws Exception {
    redisClient.shutdown();
  }
}
