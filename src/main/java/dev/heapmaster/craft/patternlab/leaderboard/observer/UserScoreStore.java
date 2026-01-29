package dev.heapmaster.craft.patternlab.leaderboard.observer;

public interface UserScoreStore {

  void incrementScore(String userId, int delta);
}
