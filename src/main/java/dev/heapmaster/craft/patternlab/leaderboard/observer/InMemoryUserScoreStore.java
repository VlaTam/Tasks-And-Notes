package dev.heapmaster.craft.patternlab.leaderboard.observer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * An in-memory implementation of {@link UserScoreStore} that stores user scores in a concurrent hash map.
 * <p>
 * This store maintains a bounded collection of user scores and notifies registered observers
 * asynchronously whenever a score is updated. The store enforces a maximum number of entries
 * to prevent unbounded growth.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Thread-safe score updates using {@link ConcurrentHashMap}</li>
 *   <li>Asynchronous observer notification to avoid blocking score updates</li>
 *   <li>Bounded capacity with configurable maximum entries</li>
 *   <li>Ignores score increments with non-positive delta values</li>
 * </ul>
 * </p>
 *
 * @see UserScoreStore
 * @see UserScoreObserver
 */
public class InMemoryUserScoreStore implements UserScoreStore {

  private final int maxEntries;
  private final Map<String, Integer> userScores;
  private final List<UserScoreObserver> observers;

  /**
   * Constructs a new {@code InMemoryUserScoreStore} with the specified maximum capacity and observers.
   *
   * @param maxEntries the maximum number of user entries allowed in the store
   * @param observers  the list of observers to notify when scores change
   */
  public InMemoryUserScoreStore(int maxEntries, List<UserScoreObserver> observers) {
    this.maxEntries = maxEntries;
    this.userScores = new ConcurrentHashMap<>();
    this.observers = observers;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Increments the score for the specified user by the given delta.
   * If the user does not exist and the store has reached its maximum capacity,
   * the increment is ignored. Non-positive delta values are also ignored.
   * </p>
   * <p>
   * Observers are notified asynchronously after the score is updated.
   * </p>
   *
   * @param userId the unique identifier of the user; must not be null
   * @param delta  the amount to increment the score by; must be positive
   * @throws NullPointerException if userId is null
   */
  @Override
  public void incrementScore(String userId, int delta) {
    Objects.requireNonNull(userId, "userId is null");
    if (delta <= 0) {
      return;
    }

    userScores.compute(userId, (id, score) -> {
      if (score == null && userScores.size() >= maxEntries) {
        return null;
      }
      var oldScore = score == null ? 0: score;
      var newScore = oldScore + delta;
      CompletableFuture.runAsync(() -> notifyObservers(userId, oldScore, newScore), Executors.newFixedThreadPool(4));
      return newScore;
    });
  }

  /**
   * Notifies all registered observers about a score change.
   *
   * @param userId   the unique identifier of the user whose score changed
   * @param oldScore the previous score value
   * @param newScore the new score value
   */
  private void notifyObservers(String userId, int oldScore, int newScore) {
    observers.forEach(observer -> observer.notify(userId, oldScore, newScore));
  }
}
