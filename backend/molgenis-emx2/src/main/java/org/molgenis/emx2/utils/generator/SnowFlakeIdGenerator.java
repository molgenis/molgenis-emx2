package org.molgenis.emx2.utils.generator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigInteger;
import java.time.Instant;
import org.molgenis.emx2.MolgenisException;

public class SnowFlakeIdGenerator {

  // It can only have one instance because of colliding ids
  // So singleton pattern or dependency injection
  private static SnowFlakeIdGenerator instance;

  private static final String BASE62_CHARACTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final long EPOCH = 1727446121186L;

  // With base62 encoding 64 bits results in a ~10 character id
  private static final int TOTAL_BITS = 64;
  private static final int TIMESTAMP_BITS = 41; // 69 years of ids
  private static final int SEQUENCE_BITS = 8; // 2^8 (256) ids per ms
  private static final int ID_BITS =
      TOTAL_BITS - TIMESTAMP_BITS - SEQUENCE_BITS; // 15 bits left for tableId ~= 32K unique hashes

  private static final long MAX_TIMESTAMP = (1L << TIMESTAMP_BITS) - 1;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
  private static final long MAX_ID = (1L << ID_BITS) - 1;

  // For the schemaId hashing
  private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
  private static final long FNV_PRIME = 0x100000001b3L;

  private long lastTimestamp = -1L;
  private long sequence = 0L;

  private SnowFlakeIdGenerator() {}

  public static synchronized SnowFlakeIdGenerator getInstance() {
    if (instance == null) {
      instance = new SnowFlakeIdGenerator();
    }
    return instance;
  }

  public synchronized String generateId(String tableId) {
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

    long tableBits = hashStringToBits(tableId);

    // Use BigInteger to avoid overflow
    BigInteger timePart = BigInteger.valueOf(currentTimestamp).shiftLeft(ID_BITS + SEQUENCE_BITS);
    BigInteger tableIdPart = BigInteger.valueOf(tableBits).shiftLeft(SEQUENCE_BITS);
    BigInteger sequencePart = BigInteger.valueOf(sequence);

    BigInteger snowflakeId = timePart.or(tableIdPart).or(sequencePart);

    return base62Encode(snowflakeId);
  }

  private String base62Encode(BigInteger snowflakeId) {
    StringBuilder encoded = new StringBuilder();
    BigInteger base = BigInteger.valueOf(BASE62_CHARACTERS.length());

    while (snowflakeId.compareTo(BigInteger.ZERO) > 0) {
      int remainder = snowflakeId.mod(base).intValue();
      encoded.insert(0, BASE62_CHARACTERS.charAt(remainder));

      snowflakeId = snowflakeId.divide(base);
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

    byte[] bytes = stringId.getBytes(UTF_8);
    for (byte b : bytes) {
      hash ^= b;
      hash *= FNV_PRIME;
    }

    return hash & MAX_ID;
  }
}
