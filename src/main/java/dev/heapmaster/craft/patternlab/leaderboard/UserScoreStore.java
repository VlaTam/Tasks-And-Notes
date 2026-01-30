package dev.heapmaster.craft.patternlab.leaderboard;

public interface UserScoreStore {

  void incrementScore(String userId, int delta);
}
