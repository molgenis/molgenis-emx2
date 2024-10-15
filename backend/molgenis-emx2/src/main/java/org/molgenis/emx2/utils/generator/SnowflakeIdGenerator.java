package org.molgenis.emx2.utils.generator;

import java.math.BigInteger;
import java.time.Instant;
import org.molgenis.emx2.MolgenisException;

public class SnowflakeIdGenerator implements IdGenerator {

  private static SnowflakeIdGenerator instance;
  private final String instanceId;

  private static final String BASE62_CHARACTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  private static final long EPOCH = 1727740800000L; // 1 October 2024 00:00:00

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

  private SnowflakeIdGenerator(String instanceId) {
    this.instanceId = instanceId;
  }

  public static synchronized SnowflakeIdGenerator getInstance() {
    if (instance == null) {
      throw new MolgenisException("SnowflakeIdGenerator not initialized");
    }
    return instance;
  }

  public static synchronized SnowflakeIdGenerator init(String instanceId) {
    if (instance != null) {
      throw new MolgenisException("SnowflakeIdGenerator is already initialized");
    }
    instance = new SnowflakeIdGenerator(instanceId);
    return instance;
  }

  public static boolean hasInstance() {
    return instance != null;
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

    long instanceIdBits = getInstanceIdBits(instanceId);

    BigInteger timePart = BigInteger.valueOf(currentTimestamp).shiftLeft(ID_BITS + SEQUENCE_BITS);
    BigInteger instancePart = BigInteger.valueOf(instanceIdBits).shiftLeft(SEQUENCE_BITS);
    BigInteger sequencePart = BigInteger.valueOf(sequence);

    BigInteger snowflakeId = timePart.or(instancePart).or(sequencePart);

    return base62Encode(snowflakeId);
  }

  public static String extractInstanceId(String snowflakeId) {
    BigInteger decodedSnowflakeId = base62Decode(snowflakeId);
    BigInteger instancePart = decodedSnowflakeId.shiftRight(SEQUENCE_BITS);
    BigInteger instanceIdBits = instancePart.and(BigInteger.valueOf(MAX_ID));

    return instanceIdBits.toString();
  }

  public static long extractTimestamp(String snowflakeId) {
    BigInteger decodedSnowflakeId = base62Decode(snowflakeId);
    BigInteger timestampPart = decodedSnowflakeId.shiftRight(ID_BITS + SEQUENCE_BITS);

    return timestampPart.longValue() + EPOCH;
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

  private static String base62Encode(BigInteger snowflakeId) {
    StringBuilder encoded = new StringBuilder();
    BigInteger base = BigInteger.valueOf(BASE62_CHARACTERS.length());

    while (snowflakeId.compareTo(BigInteger.ZERO) > 0) {
      int remainder = snowflakeId.mod(base).intValue();
      encoded.insert(0, BASE62_CHARACTERS.charAt(remainder));

      snowflakeId = snowflakeId.divide(base);
    }

    return encoded.toString();
  }

  private static BigInteger base62Decode(String encoded) {
    BigInteger result = BigInteger.ZERO;
    BigInteger base = BigInteger.valueOf(BASE62_CHARACTERS.length());

    for (int i = 0; i < encoded.length(); i++) {
      int charIndex = BASE62_CHARACTERS.indexOf(encoded.charAt(i));
      if (charIndex == -1) {
        throw new IllegalArgumentException("Invalid character in Base62 encoded string");
      }
      result = result.multiply(base).add(BigInteger.valueOf(charIndex));
    }

    return result;
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

  public String getInstanceId() {
    return this.instanceId;
  }
}
