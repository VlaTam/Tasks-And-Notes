package dev.heapmaster.craft.patternlab.urlshortener;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple URL shortener service that encodes long URLs into short URLs and decodes them back.
 * It uses Base62 encoding for generating short URL paths.
 */
public class UrlShortener {

  /**
   * The length of the generated short URL path.
   * <p>
   * Capacity estimation:
   * <ul>
   *   <li>Expected load: 10 million write requests per day</li>
   *   <li>Read/write ratio: 100:1 (i.e., ~1 billion read requests per day)</li>
   *   <li>Yearly write volume: ~3.65 billion requests (10M × 365)</li>
   *   <li>5-year write volume: ~18.25 billion requests</li>
   *   <li>Base62 with 6 characters provides ~56.8 billion unique combinations (62^6)</li>
   * </ul>
   * This gives us sufficient headroom to handle growth beyond the 5-year projection.
   * Note: Only write requests consume unique codes; reads are lookups only.
   */
  private static final int SHORT_URL_LENGTH = 6;
  private static final String DOMAIN = "http://tinyurl.com/";
  private static final String PATTERN = "^https?://.*";
  private final AtomicLong counter = new AtomicLong(0);
  private final Map<String, String> longAndShortUrls = new ConcurrentHashMap<>();
  private final Map<String, String> shortAndLongUrls = new ConcurrentHashMap<>();

  /**
   * Shortens a long URL to a short URL (returns the full short URL like "http://tinyurl.com/abc123").
   * If the URL has already been shortened — returns the existing short URL.
   */
  String encode(String longUrl) {
    if (longUrl == null || longUrl.isBlank() || !longUrl.matches(PATTERN)) {
      throw new IllegalArgumentException("Invalid url: " + longUrl);
    }

    var existing = longAndShortUrls.get(longUrl);
    if (existing != null) {
      return existing;
    }

    String shortenedUrl;
    do {
      var input = counter.incrementAndGet();
      var encoded = Base62.encode(input);

      shortenedUrl = DOMAIN + normalizeLength(encoded);
    } while (isShortenedUrlExists(shortenedUrl));

    var previous = longAndShortUrls.putIfAbsent(longUrl, shortenedUrl);
    if (previous != null) {
      return previous;
    }
    shortAndLongUrls.putIfAbsent(shortenedUrl, longUrl);
    return shortenedUrl;
  }

  private String normalizeLength(String input) {
    if (input.length() < SHORT_URL_LENGTH) {
      return input + Base62.generateRandomString(SHORT_URL_LENGTH - input.length());
    }
    if (input.length() > SHORT_URL_LENGTH) {
      return input.substring(0, SHORT_URL_LENGTH);
    }
    return input;
  }

  private boolean isShortenedUrlExists(String shortenedUrl) {
    return shortAndLongUrls.containsKey(shortenedUrl);
  }

  /**
   * Decodes the short URL back to the original long URL.
   * @throws IllegalArgumentException if the shortUrl is invalid or not found.
   */
  String decode(String shortUrl) {
    if (shortUrl == null || shortUrl.isBlank() || !shortUrl.startsWith(DOMAIN)) {
      throw new IllegalArgumentException("Invalid url: " + shortUrl);
    }

    var longUrl = shortAndLongUrls.get(shortUrl);
    if (longUrl == null) {
      throw new IllegalArgumentException("Url %s not found".formatted(shortUrl));
    }

    return longUrl;
  }

  private static class Base62 {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();
    private static final Random RANDOM = ThreadLocalRandom.current();

    public static String encode(long input) {
      if (input < 0) {
        throw new IllegalArgumentException("Input cannot be negative");
      }

      if (input == 0) {
        return String.valueOf(ALPHABET.charAt(0));
      }

      StringBuilder encoded = new StringBuilder();
      while (input > 0) {
        var index = input % BASE;
        encoded.append(ALPHABET.charAt((int) index));
        input /= BASE;
      }

      return encoded.reverse().toString();
    }

    public static String generateRandomString(int length) {
      byte[] bytes = new byte[length];
      RANDOM.nextBytes(bytes);
      StringBuilder stringBuilder = new StringBuilder(length);
      for (byte b : bytes) {
        var ch = ALPHABET.charAt(b & 0x3F); // Keep only 6 bits to fit into Base62 range
        stringBuilder.append(ch);
      }
      return stringBuilder.toString();
    }
  }

  public static void main(String[] args) {
    UrlShortener urlShortener = new UrlShortener();
    String longUrl = "https://www.example.com/some/very/long/path";
    String shortUrl = urlShortener.encode(longUrl);
    System.out.println("Shortened URL: " + shortUrl);

    String decodedUrl = urlShortener.decode(shortUrl);
    System.out.println("Decoded URL: " + decodedUrl);
  }
}
