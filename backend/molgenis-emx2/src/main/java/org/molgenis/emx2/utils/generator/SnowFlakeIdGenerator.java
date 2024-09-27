package org.molgenis.emx2.utils.generator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigInteger;
import java.time.Instant;
import org.molgenis.emx2.MolgenisException;

public class SnowFlakeIdGenerator implements IdGenerator {

  private static final String BASE62_CHARACTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private static final long EPOCH = 1727446121186L;

  // With base62 encoding 64 bits results in a ~10 character id
  private static final int TOTAL_BITS = 64;
  private static final int TIMESTAMP_BITS = 41; // 69 years of ids
  private static final int SEQUENCE_BITS = 8; // 2^8 (256) ids per ms
  private static final int HASHED_STRING_BITS =
      TOTAL_BITS - TIMESTAMP_BITS - SEQUENCE_BITS; // 15 bits left for tableId ~= 32K unique hashes

  private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
  private static final long MAX_HASHED_STRING_ID = (1L << HASHED_STRING_BITS) - 1;

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

    // Use BigInteger to avoid overflow
    BigInteger timestampPart =
        BigInteger.valueOf(currentTimestamp).shiftLeft(HASHED_STRING_BITS + SEQUENCE_BITS);
    BigInteger tablePart = BigInteger.valueOf(tableBits).shiftLeft(SEQUENCE_BITS);
    BigInteger sequencePart = BigInteger.valueOf(sequence);

    BigInteger snowflakeId = timestampPart.or(tablePart).or(sequencePart);

    return base62EncodeSnowflakeId(snowflakeId.longValue());
  }

  private String base62EncodeSnowflakeId(long snowflakeId) {
    StringBuilder encoded = new StringBuilder();
    while (snowflakeId > 0) {
      int remainder = (int) (snowflakeId % BASE62_CHARACTERS.length());
      encoded.insert(0, BASE62_CHARACTERS.charAt(remainder)); // Prepend character instead of append
      snowflakeId /= BASE62_CHARACTERS.length();
    }
    return encoded.toString();
  }

  private long getCurrentTimestamp() {
    long now = Instant.now().toEpochMilli();
    long currentTimestamp = now - EPOCH;

    if (currentTimestamp > MAX_TIMESTAMP) {
      throw new MolgenisException("Snowflake id too old");
    }
    return currentTimestamp;
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
}
