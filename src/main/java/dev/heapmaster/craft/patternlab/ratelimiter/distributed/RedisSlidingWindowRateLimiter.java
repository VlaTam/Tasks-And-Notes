package dev.heapmaster.craft.patternlab.ratelimiter.distributed;

import dev.heapmaster.craft.patternlab.ratelimiter.RateLimiter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Instant;

public class RedisSlidingWindowRateLimiter implements RateLimiter, AutoCloseable {

  private static final String LUA_SCRIPT = """
      local key = KEYS[1]
      local limit = tonumber(ARGV[1])
      local window_ms = tonumber(ARGV[2])
      local now_ms = tonumber(ARGV[3])
      
      redis.call('ZREMRANGEBYSCORE', key, 0, now_ms - window_ms)
      local current = redis.call('ZCARD', key)
      
      if current < limit then
          redis.call('ZADD', key, now_ms, now_ms)
          redis.call('PEXPIRE', key, window_ms)
          return 1
      end
      return 0
      """;
  private static final String NAMESPACE = "rate:";

  private final int maxRequests;
  private final int windowSizeInSeconds;

  private final RedisClient redisClient;
  private final RedisCommands<String, String> syncCommands;
  private final String scriptSHA;  // SHA1 хэш загруженного скрипта

  public RedisSlidingWindowRateLimiter(int maxRequests, int windowSizeInSeconds) {
    this.maxRequests = maxRequests;
    this.windowSizeInSeconds = windowSizeInSeconds;

    RedisURI uri = RedisURI.Builder.redis("localhost", 6379).build();
    this.redisClient = RedisClient.create(uri);
    this.syncCommands = redisClient.connect().sync();
    scriptSHA = this.syncCommands.scriptLoad(LUA_SCRIPT);
  }

  @Override
  public boolean allowRequest(String userId) {
    var now = Instant.now().toEpochMilli();
    var key = NAMESPACE + userId;
    long result = syncCommands.evalsha(scriptSHA, ScriptOutputType.INTEGER,
        new String[]{key},
        String.valueOf(maxRequests),
        String.valueOf(windowSizeInSeconds * 1000L),
        String.valueOf(now));
    return result == 1;
  }

  @Override
  public void close() {
    redisClient.close();
  }
}
