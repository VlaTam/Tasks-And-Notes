package dev.heapmaster.craft.patternlab.leaderboard.observer;

import java.util.List;

public interface UserScoreObserver {

  void notify(String userId, int oldScore, int newScore);

  List<String> getTopN(int n);
}
