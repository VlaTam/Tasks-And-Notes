package dev.heapmaster.craft.patternlab.leaderboard;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class InMemoryLeaderboard implements Leaderboard {

  private final int maxEntries;
  private final Map<String, Integer> scores;
  private final SortedSet<UserScore> sortedScores;

  public InMemoryLeaderboard(int maxEntries) {
    if (maxEntries <= 0) {
      throw new IllegalArgumentException("maxEntries must be positive");
    }
    this.maxEntries = maxEntries;
    this.scores = new HashMap<>();
    this.sortedScores = new TreeSet<>(Comparator.comparing(UserScore::score).reversed().thenComparing(UserScore::userId));
  }

  @Override
  public void incrementScore(String userId, int delta) {
    Objects.requireNonNull(userId, "userId must not be null");
    if (delta <= 0) {
      return;
    }

    checkIfFull(userId);
    var oldValue = scores.get(userId);

    if (oldValue != null) {
      sortedScores.remove(new UserScore(userId, oldValue));
    }
    int newValue = (oldValue == null ? 0 : oldValue) + delta;
    scores.put(userId, newValue);
    sortedScores.add(new UserScore(userId, newValue));
  }

  private void checkIfFull(String userId) {
    if (!scores.containsKey(userId) && scores.size() >= maxEntries) {
      throw new IllegalArgumentException("New user can't be added, leaderboard is full");
    }
  }

  @Override
  public List<String> getTopN(int n) {
    if (n > maxEntries || n <= 0) {
      throw new IllegalArgumentException("maxEntries must be positive and less than or equal to " + maxEntries);
    }
    return sortedScores.stream().limit(n).map(UserScore::userId).toList();
  }

  private record UserScore(String userId, int score) {
  }
}
