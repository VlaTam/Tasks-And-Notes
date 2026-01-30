package dev.heapmaster.craft.patternlab.leaderboard.observer;

import dev.heapmaster.craft.patternlab.leaderboard.UserScoreObserver;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An in-memory implementation of {@link UserScoreObserver} that maintains a sorted set of user scores.
 * <p>
 * This observer keeps track of user scores in a {@link TreeSet}, sorted by score in descending order.
 * When scores are equal, users are sorted alphabetically by their user ID.
 * </p>
 * <p>
 * This implementation is thread-safe for the {@link #update(String, int, int)} method.
 * </p>
 *
 * @see UserScoreObserver
 */
public class InMemoryUserScoreObserver implements UserScoreObserver {

  private final SortedSet<UserScore> userScores;

  /**
   * Constructs a new {@code InMemoryUserScoreObserver} with an empty score set.
   */
  public InMemoryUserScoreObserver() {
    this.userScores = new TreeSet<>(Comparator.comparing(UserScore::score).reversed().thenComparing(UserScore::userId));
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method is synchronized to ensure thread-safe updates to the internal score set.
   * It removes the old score entry and adds a new one with the updated score.
   * </p>
   */
  @Override
  public synchronized void update(String userId, int oldScore, int newScore) {
    userScores.remove(new UserScore(userId, oldScore));
    userScores.add(new UserScore(userId, newScore));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the top N users sorted by score in descending order.
   * If N is greater than the number of users, returns all users.
   * </p>
   */
  @Override
  public List<String> getTopN(int n) {
    return userScores.stream().limit(Math.clamp(n, 0, userScores.size())).map(UserScore::userId).toList();
  }

  /**
   * Internal record representing a user's score.
   *
   * @param userId the unique identifier of the user
   * @param score  the user's current score
   */
  private record UserScore(String userId, int score) {
  }
}
