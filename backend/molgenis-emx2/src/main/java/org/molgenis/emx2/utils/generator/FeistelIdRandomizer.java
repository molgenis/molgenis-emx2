package org.molgenis.emx2.utils.generator;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Shuffles sequential numbers (0, 1, 2, ...) into a seemingly random order, while guaranteeing:
 *
 * <ul>
 *   <li>Every input maps to exactly one unique output (no duplicates)
 *   <li>Every output can be reversed back to the original input
 *   <li>All outputs stay within [0, domain)
 * </ul>
 *
 * <p>Uses a <a href="https://en.wikipedia.org/wiki/Feistel_cipher">Feistel cipher</a> — a technique
 * that splits a number into two halves and scrambles them over multiple rounds. Combined with
 * "cycle walking" to reject values outside the domain, this creates a perfect 1-to-1 shuffle over
 * any domain size.
 *
 * <p>Example: with domain=1000, the sequence 0,1,2,...,999 gets mapped to a shuffled sequence like
 * 473,891,12,...,307 where every number 0-999 appears exactly once.
 */
public class FeistelIdRandomizer implements IdRandomizer {

  /** More rounds = more scrambling. 4 is standard for format-preserving encryption. */
  private static final int ROUNDS = 4;

  /** The upper bound (exclusive) — all inputs and outputs must be in [0, domain). */
  private final long domain;

  /**
   * The number is split into two halves for the Feistel rounds. This is how many bits each half
   * uses. For domain=1000 we need 10 bits total (2^10=1024 >= 1000), so each half is 5 bits.
   */
  private final int halfBits;

  /**
   * A bitmask to extract one half from a number. For halfBits=5 this is 0b11111 = 31, meaning "keep
   * only the lowest 5 bits".
   */
  private final long halfMask;

  private final SecretKeySpec aesKey;

  public FeistelIdRandomizer(long domain, byte[] key) {
    if (domain < 2) {
      throw new IllegalArgumentException("Domain must be at least 2");
    }
    this.domain = domain;

    // How many bits do we need to represent all values in [0, domain)?
    // Example: domain=1000 -> we need 10 bits (2^10 = 1024 >= 1000)
    int totalBits = Long.SIZE - Long.numberOfLeadingZeros(domain - 1);

    // Split into two halves (rounding up so the halves can cover all values)
    this.halfBits = (totalBits + 1) / 2;

    // Create bitmask: e.g. halfBits=5 -> halfMask = 0b11111 = 31
    this.halfMask = (1L << halfBits) - 1;

    this.aesKey = new SecretKeySpec(key, "AES");
  }

  /** Scrambles a sequential input into a pseudo-random output within [0, domain). */
  @Override
  public long randomize(long input) {
    return cycleWalk(input, true);
  }

  /** Reverses a scrambled value back to its original sequential input. */
  public long reverse(long input) {
    return cycleWalk(input, false);
  }

  /**
   * The Feistel cipher works on a power-of-2 range (e.g. 0-1023 for 10 bits), but our domain might
   * be smaller (e.g. 0-999). Cycle walking simply re-applies the cipher whenever the result falls
   * outside our domain, until it lands inside. This is guaranteed to terminate because the cipher
   * is a 1-to-1 mapping.
   */
  private long cycleWalk(long value, boolean forward) {
    long result = forward ? feistel(value) : inverseFeistel(value);
    if (result >= domain) {
      return cycleWalk(result, forward);
    }
    return result;
  }

  /**
   * The core Feistel cipher. Splits the input into left and right halves, then for each round:
   *
   * <ol>
   *   <li>Generate a pseudo-random number from the right half (using AES)
   *   <li>XOR it with the left half to scramble it
   *   <li>Swap left and right
   * </ol>
   *
   * After all rounds, recombine the halves into a single number.
   */
  private long feistel(long input) {
    long left = extractLeftHalf(input);
    long right = extractRightHalf(input);

    for (int round = 0; round < ROUNDS; round++) {
      long scrambled = left ^ pseudoRandomFunction(right, round);
      left = right;
      right = scrambled;
    }

    return combineHalves(left, right);
  }

  /**
   * Reverses the Feistel cipher by running the rounds backwards. Because each round is just a swap
   * + XOR, we can undo each step by applying the same XOR in reverse order.
   */
  private long inverseFeistel(long input) {
    long left = extractLeftHalf(input);
    long right = extractRightHalf(input);

    for (int round = ROUNDS - 1; round >= 0; round--) {
      long unscrambled = right ^ pseudoRandomFunction(left, round);
      right = left;
      left = unscrambled;
    }

    return combineHalves(left, right);
  }

  private long extractLeftHalf(long value) {
    return (value >> halfBits) & halfMask;
  }

  private long extractRightHalf(long value) {
    return value & halfMask;
  }

  private long combineHalves(long left, long right) {
    return (left << halfBits) | right;
  }

  /**
   * Produces a deterministic pseudo-random number from a value and round number. Uses AES
   * encryption as the source of randomness — given the same value and round, it always returns the
   * same result, but small changes in input produce completely different outputs.
   */
  private long pseudoRandomFunction(long value, int round) {
    byte[] block = longToAesBlock(value, round);

    try {
      Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
      aes.init(Cipher.ENCRYPT_MODE, aesKey);
      byte[] encrypted = aes.doFinal(block);
      return bytesToLong(encrypted) & halfMask;
    } catch (Exception e) {
      throw new RuntimeException("AES encryption failed", e);
    }
  }

  /** Packs a value and round number into a 16-byte AES input block. */
  private static byte[] longToAesBlock(long value, int round) {
    byte[] block = new byte[16];
    block[0] = (byte) round;
    for (int i = 0; i < 8; i++) {
      block[1 + i] = (byte) ((value >> (i * 8)) & 0xFF);
    }
    return block;
  }

  /** Reads the first 8 bytes of an array as a long value. */
  private static long bytesToLong(byte[] bytes) {
    long result = 0;
    for (int i = 0; i < 8; i++) {
      result = (result << 8) | (bytes[i] & 0xFF);
    }
    return result;
  }
}
