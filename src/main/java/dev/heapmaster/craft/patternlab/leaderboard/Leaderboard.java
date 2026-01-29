package dev.heapmaster.craft.patternlab.leaderboard;

import java.util.List;

public interface Leaderboard {

  void incrementScore(String userId, int delta);
  List<String> getTopN(int n);
}
