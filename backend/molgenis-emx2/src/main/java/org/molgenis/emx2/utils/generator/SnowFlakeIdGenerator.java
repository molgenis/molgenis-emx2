package org.molgenis.emx2.utils.generator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

public class SnowFlakeIdGenerator implements IdGenerator {

  private static final String BASE62_CHARACTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private static final long EPOCH = 1706208000000L;

  private static final int TOTAL_BITS = 64; // With base62 id = 10 character long
  private static final int TIMESTAMP_BITS = 41; // 69 years of ids
  private static final int SEQUENCE_BITS = 8; // 2^8 ids per ms
  private static final int HASHED_STRING_BITS =
      TOTAL_BITS - TIMESTAMP_BITS - SEQUENCE_BITS; // 15 bits left for tableId ~= 32K unique hashes

  private static final long MAX_HASHED_STRING_ID = (1L << HASHED_STRING_BITS) - 1;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

  // For the schemaId hashing
  private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;
  private final long tableBits;

  private long lastTimestamp = -1L;
  private long sequence = 0L;

  public SnowFlakeIdGenerator(String tableId) {
    this.tableBits = hashStringToBits(tableId);
  }

  public synchronized String generateId() {
    long currentTimestamp = getCurrentTimestamp();

    if (currentTimestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        currentTimestamp = waitForNextMillis(currentTimestamp);
      }
    } else {
      sequence = 0;
    }

    lastTimestamp = currentTimestamp;

    long snowflakeId =
        ((currentTimestamp & ((1L << TIMESTAMP_BITS) - 1)) << (HASHED_STRING_BITS + SEQUENCE_BITS))
            | (tableBits << SEQUENCE_BITS)
            | sequence;

    return base62EncodeSnowflakeId(snowflakeId);
  }

  private String base62EncodeSnowflakeId(long snowflakeId) {
    StringBuilder encoded = new StringBuilder();
    while (snowflakeId > 0) {
      int remainder = (int) (snowflakeId % 62);
      encoded.append(BASE62_CHARACTERS.charAt(remainder));
      snowflakeId /= 62;
    }
    return encoded.reverse().toString();
  }

  private long getCurrentTimestamp() {
    return Instant.now().toEpochMilli() - EPOCH;
  }

  private long waitForNextMillis(long currentTimestamp) {
    long newTimestamp = getCurrentTimestamp();
    while (newTimestamp <= currentTimestamp) {
      newTimestamp = getCurrentTimestamp();
    }
    return newTimestamp;
  }

  private long hashStringToBits(String stringId) {
    long hash = FNV_OFFSET_BASIS;

    // Convert the string to bytes and calculate the hash
    byte[] bytes = stringId.getBytes(UTF_8);
    for (byte b : bytes) {
      hash ^= b;
      hash *= FNV_PRIME;
    }

    return hash & MAX_HASHED_STRING_ID;
  }

  public static void main(String[] args) throws NoSuchAlgorithmException {
    SnowFlakeIdGenerator snowflake = new SnowFlakeIdGenerator("schemaId");

    int totalIds = 10000;

    List<String> ids = new ArrayList<>();

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < totalIds; i++) {
      String snowflakeId = snowflake.generateId();
      ids.add(snowflakeId);
    }
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime; // duration in milliseconds
    System.out.println("Execution time: " + duration + " ms");

    Set<String> uniqueIds = new HashSet<>(ids);
    if (uniqueIds.size() == ids.size()) {
      System.out.println("All IDs are unique");
    } else {
      System.out.println("Duplicate IDs found");
    }

    // Check if IDs are sorted
    List<String> sortedIds = new ArrayList<>(ids);
    Collections.sort(sortedIds);
    if (ids.equals(sortedIds)) {
      System.out.println("IDs are sorted");
    } else {
      System.out.println("IDs are not sorted");
    }
  }
}
