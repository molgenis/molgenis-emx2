package org.molgenis.sql;

public class StopWatch {

  private static long time = System.currentTimeMillis();

  public static void print(String message) {
    long endTime = System.currentTimeMillis();
    System.out.println((endTime - time) + "ms: " + message);
    time = System.currentTimeMillis();
  }

  public static void start(String message) {
    System.out.println(message);
    time = System.currentTimeMillis();
  }

  public static void print(String message, int count) {
    long endTime = System.currentTimeMillis();
    long total = (endTime - time);
    System.out.println(total + "ms: " + message + " (" + 1000 * count / total + " per second)");
    time = System.currentTimeMillis();
  }
}
