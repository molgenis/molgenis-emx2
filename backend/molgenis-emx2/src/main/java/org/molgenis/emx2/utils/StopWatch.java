package org.molgenis.emx2.utils;

public class StopWatch {

  private StopWatch() { // hides public constructor
  }

  private static long time = System.currentTimeMillis();

  public static void print(String message) {
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - time) + "ms: " + message); // NOSONAR
    time = System.currentTimeMillis();
  }

  public static void start(String message) {
    System.out.println("start: " + message); // NOSONAR
    time = System.currentTimeMillis();
  }

  public static void print(String message, int count) {
    long endTime = System.currentTimeMillis();
    long total = (endTime - time);
    if (total == 0) total = 1;
    System.out.println( // NOSONAR
        total
            + "ms: "
            + message
            + " (count="
            + count
            + " with "
            + 1000 * count / total
            + " per second)");
    time = System.currentTimeMillis();
  }
}
