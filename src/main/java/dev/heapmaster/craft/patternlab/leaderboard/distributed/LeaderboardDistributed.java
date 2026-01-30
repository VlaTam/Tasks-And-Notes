package dev.heapmaster.craft.patternlab.leaderboard.distributed;

import dev.heapmaster.craft.patternlab.leaderboard.Leaderboard;
import dev.heapmaster.craft.patternlab.leaderboard.UserScoreObserver;
import dev.heapmaster.craft.patternlab.leaderboard.UserScoreStore;

import java.util.List;

public class LeaderboardDistributed implements Leaderboard, AutoCloseable {

  private final UserScoreObserver observer;
  private final UserScoreStore userScores;

  public LeaderboardDistributed() {
    this.observer = new RedisUserScoreObserver();
    this.userScores = new RedisUserScoreStore(List.of(observer));
  }

  @Override
  public void incrementScore(String userId, int delta) {
    userScores.incrementScore(userId, delta);
  }

  @Override
  public List<String> getTopN(int n) {
    return observer.getTopN(n);
  }

  @Override
  public void close() throws Exception {
    ((RedisUserScoreObserver) observer).close();
    ((RedisUserScoreStore) userScores).close();
  }
}
