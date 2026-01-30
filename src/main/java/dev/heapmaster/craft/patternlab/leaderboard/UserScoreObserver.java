package dev.heapmaster.craft.patternlab.leaderboard;

import java.util.List;

public interface UserScoreObserver {

  default void update(String userId, int oldScore, int newScore) {
  }

  default void update(String userId, int delta) {
  }

  List<String> getTopN(int n);
}
