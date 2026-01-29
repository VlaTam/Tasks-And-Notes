package dev.heapmaster.craft.patternlab.leaderboard.observer;

import dev.heapmaster.craft.patternlab.leaderboard.Leaderboard;

import java.util.List;

/**
 * An implementation of {@link Leaderboard} that uses the Observer pattern to maintain rankings.
 * <p>
 * This leaderboard delegates score storage to an {@link InMemoryUserScoreStore} and ranking
 * queries to an {@link InMemoryUserScoreObserver}. When scores are updated, the store
 * automatically notifies the observer, which maintains a sorted view of user rankings.
 * </p>
 * <p>
 * This design separates concerns:
 * <ul>
 *   <li>The store handles score persistence and updates</li>
 *   <li>The observer handles ranking and top-N queries</li>
 * </ul>
 * </p>
 *
 * @see Leaderboard
 * @see InMemoryUserScoreStore
 * @see InMemoryUserScoreObserver
 */
public class ObserverLeaderboard implements Leaderboard {

  private final UserScoreObserver observer;
  private final UserScoreStore userScores;

  /**
   * Constructs a new {@code ObserverLeaderboard} with the specified maximum number of entries.
   *
   * @param maxEntries the maximum number of user entries allowed in the leaderboard
   */
  public ObserverLeaderboard(int maxEntries) {
    this.observer = new InMemoryUserScoreObserver();
    this.userScores = new InMemoryUserScoreStore(maxEntries, List.of(observer));
  }

  /**
   * {@inheritDoc}
   * <p>
   * Increments the score for the specified user. The observer is notified asynchronously
   * to update the rankings.
   * </p>
   */
  @Override
  public void incrementScore(String userId, int delta) {
    userScores.incrementScore(userId, delta);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the top N users by score, retrieved from the observer which maintains
   * a sorted view of all user scores.
   * </p>
   */
  @Override
  public List<String> getTopN(int n) {
    return observer.getTopN(n);
  }
}
