package org.molgenis.emx2.utils.generator;

import java.math.BigInteger;
import java.time.Instant;
import org.molgenis.emx2.MolgenisException;

public class SnowFlakeIdGenerator {

  private static SnowFlakeIdGenerator instance;

  private static final String BASE62_CHARACTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final long EPOCH = 1727446121186L;

  public static final int TOTAL_BITS = 64;
  public static final int TIMESTAMP_BITS = 41; // 69 years of ids
  public static final int SEQUENCE_BITS = 8; // 2^8 (256) ids per ms
  public static final int ID_BITS =
      TOTAL_BITS
          - TIMESTAMP_BITS
          - SEQUENCE_BITS; // 15 bits left for the instanceId ~= 32K unique hashes

  public static final long MAX_TIMESTAMP = maxValueForBits(TIMESTAMP_BITS);
  public static final long MAX_SEQUENCE = maxValueForBits(SEQUENCE_BITS);
  public static final long MAX_ID = maxValueForBits(ID_BITS);

  private long lastTimestamp = -1L;
  private long sequence = 0L;

  private SnowFlakeIdGenerator() {}

  public static synchronized SnowFlakeIdGenerator getInstance() {
    if (instance == null) {
      instance = new SnowFlakeIdGenerator();
    }
    return instance;
  }

  public synchronized String generateId(String instanceId) {
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

    long instanceIdBits = getInstanceIdBits(instanceId);

    BigInteger timePart = BigInteger.valueOf(currentTimestamp).shiftLeft(ID_BITS + SEQUENCE_BITS);
    BigInteger instancePart = BigInteger.valueOf(instanceIdBits).shiftLeft(SEQUENCE_BITS);
    BigInteger sequencePart = BigInteger.valueOf(sequence);

    BigInteger snowflakeId = timePart.or(instancePart).or(sequencePart);

    return base62Encode(snowflakeId);
  }

  private static long getInstanceIdBits(String instanceId) {
    long instanceIdBits = Long.parseLong(instanceId);
    if (instanceIdBits > MAX_ID) {
      throw new MolgenisException("Instance id too large: " + instanceId);
    }
    return instanceIdBits;
  }

  private static long maxValueForBits(int bits) {
    return (1L << bits) - 1;
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
}
