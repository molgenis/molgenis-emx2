package org.molgenis.utils;

public class StopWatch {

  private StopWatch() { // hides public constructor
  }

  private static long time = System.currentTimeMillis();

  public static void print(String message) {
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - time) + "ms: " + message); // nosonar
    time = System.currentTimeMillis();
  }

  public static void start(String message) {
    System.out.println("start: " + message); // nosonar
    time = System.currentTimeMillis();
  }

  public static void print(String message, int count) {
    long endTime = System.currentTimeMillis();
    long total = (endTime - time);
    if (total == 0) total = 1;
    System.out.println(
        total
            + "ms: "
            + message
            + " (count="
            + count
            + " with "
            + 1000 * count / total
            + " per second)"); // nosonar
    time = System.currentTimeMillis();
  }
}
