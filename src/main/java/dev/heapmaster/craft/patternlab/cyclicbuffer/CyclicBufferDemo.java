package dev.heapmaster.craft.patternlab.cyclicbuffer;

// Iterative chunk reader

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class contains the logic for transferring data from a cyclic buffer to a consumer.
 * Task:
 * Data is written to the CyclicBuffer. The write position may "wrap around" the buffer boundary (1GB).
 * We read data from the last read position up to the current write position. The current position is not included in the read data.
 * The intensity at which data is written to the buffer is unknown. For example, data may not be written for an hour, and then suddenly gigabytes may be written.
 * It is necessary to implement logic in the CyclicBufferDemo class that will correctly read data from the buffer and pass it to the consumer.
 */
public class CyclicBufferDemo {

  private static final int PAGE_SIZE = 4 * 1024; // 4KB
  private static final int TOTAL_BYTES = 1024 * 1024 * 1024; // 1GB
  private int prevPos;
  private final AtomicInteger prevPosAtomic;
  private final ThreadLocal<byte[]> resBytesThreadLocal;
  private final byte[] resBytes;
  private final CyclicBuffer buffer;
  private final Consumer consumer;

  public CyclicBufferDemo(CyclicBuffer buffer, Consumer consumer) {
    this.prevPosAtomic = new AtomicInteger(0);
    this.resBytesThreadLocal = ThreadLocal.withInitial(() -> new byte[PAGE_SIZE]);
    this.prevPos = 0;
    this.resBytes = new byte[PAGE_SIZE];
    this.buffer = buffer;
    this.consumer = consumer;
  }

  /**
   * Transfers data from the cyclic buffer to the consumer using a spin lock mechanism.
   * The method reads data from the last read position up to the current write position,
   * handling the wrap-around of the buffer boundary (1GB).
   */
  public void transferSpinLock() {
    int currentPos;
    int myPrevPos;
    do {
      currentPos = buffer.getCurrentPosition();
      myPrevPos = prevPosAtomic.get();

      if (currentPos == myPrevPos) {
        return;
      }
    } while(!prevPosAtomic.compareAndSet(myPrevPos, currentPos));

    int toRead;
    if (currentPos > myPrevPos) {
      toRead = currentPos - myPrevPos;
    } else {
      toRead = (TOTAL_BYTES - myPrevPos) + currentPos;
    }
    int written = 0;
    int offset = 0;

    while (written < toRead) {
      int chunkGlobalPos = myPrevPos + offset;
      if (chunkGlobalPos >= TOTAL_BYTES) {
        chunkGlobalPos -= TOTAL_BYTES;
      }

      int pageNum = chunkGlobalPos / PAGE_SIZE;
      byte[] pageBytes = buffer.getPage(pageNum);

      int initPositionInPage = chunkGlobalPos % PAGE_SIZE;

      int bytesTillEndOfPage = PAGE_SIZE - initPositionInPage;
      int remaining = toRead - written;
      int lengthToCopy = Math.min(bytesTillEndOfPage, remaining);

      byte[] bytesToSend = resBytesThreadLocal.get();
      if (lengthToCopy < PAGE_SIZE) {
        bytesToSend = new byte[lengthToCopy];
      }

      System.arraycopy(pageBytes, initPositionInPage, bytesToSend, 0, lengthToCopy);
      consumer.handle(bytesToSend);

      written += lengthToCopy;
      offset += lengthToCopy;
    }
    prevPosAtomic.set(currentPos);
    resBytesThreadLocal.remove();
  }

  /**
   * Transfers data from the cyclic buffer to the consumer in a single thread.
   * The method reads data from the last read position up to the current write position,
   * handling the wrap-around of the buffer boundary (1GB).
   */
  public void transferSingleThread() {
    int currentPos = buffer.getCurrentPosition();
    if (prevPos == currentPos) {
      return;
    }
    int toRead = currentPos - prevPos;
    if (prevPos > currentPos) {
      toRead = (TOTAL_BYTES - prevPos) + currentPos;
    }
    int written = 0;
    int offset = 0;

    while (written < toRead) {
      int globalChunkPos = (prevPos + offset) % TOTAL_BYTES;
      int pageNumber = globalChunkPos / PAGE_SIZE;
      byte[] pageBytes = buffer.getPage(pageNumber);

      int initialPositionOnPage = globalChunkPos % PAGE_SIZE;
      int remaining = toRead - written;
      int bytesNumberToCopy = Math.min(PAGE_SIZE - initialPositionOnPage, remaining);

      byte[] bytesToCopy = resBytes;
      if (bytesNumberToCopy < PAGE_SIZE) {
        bytesToCopy = new byte[bytesNumberToCopy];
      }
      System.arraycopy(pageBytes, initialPositionOnPage, bytesToCopy, 0, bytesNumberToCopy);
      consumer.handle(bytesToCopy);

      written += bytesNumberToCopy;
      offset += bytesNumberToCopy;
    }

    prevPos = currentPos;
  }

  public static void main(String[] args) {

  }

  /**
   * Cyclic buffer of fixed size 1GB for storing byte pages.
   * Each page has a size of 4KB.
   * The class provides methods to get the current position and the content of a page by its number.
   * The current position points to the current byte in the buffer. It can be in the range from 0 to 1GB-1.
   */
  public static class CyclicBuffer {

    /**
     * Returns the position up to which data has been written to the buffer.
     * The method signature must not be changed.
     * @return the current write position (value from 0 to 1GB-1)
     */
    public int getCurrentPosition() {
      return 0; // Value from 0 to 1GB-1
    }

    /**
     * Returns the content of the page by its number.
     * The method signature must not be changed.
     * @param pageNumber the page number
     * @return a byte array of size 4KB representing the content of the page
     */
    public byte[] getPage(int pageNumber) {
      return new byte[1024 * 4];  // Page content of size 4KB
    }
  }

  /**
   * Consumer of data from the cyclic buffer.
   * Provides a method for processing byte data.
   */
  public static class Consumer {

    /**
     * The method signature must not be changed.
     * @param bytes the byte data to process
     */
    void handle(byte[] bytes) {
      // Some implementation for processing byte data
    }
  }
}
