package org.molgenis.emx2.utils.generator;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.util.encoders.Hex;

public class FeistelIdRandomizer implements IdRandomizer {

  private static final byte[] KEY = Hex.decode("2B7E151628AED2A6ABF7158809CF4F3C");
  private static final int ROUNDS = 4;
  private static final int HALF_BITS = 5;
  private static final long HALF_MASK = (1L << HALF_BITS) - 1;

  private final long domain;

  private final SecretKeySpec aesKey;

  public FeistelIdRandomizer(long domain) {
    this.domain = domain;
    this.aesKey = new SecretKeySpec(KEY, "AES");
  }

  @Override
  public long randomize(long input) {
    return cycleWalk(input);
  }

  private long cycleWalk(long x) {
    long result = feistel(x);
    if (result >= domain) {
      return cycleWalk(result);
    }
    return result;
  }

  private long feistel(long input) {
    long left = (input >> HALF_BITS) & HALF_MASK;
    long right = input & HALF_MASK;

    for (int round = 0; round < ROUNDS; round++) {
      long newL = right;
      long newR = left ^ aesPrf(right, round);
      left = newL;
      right = newR;
    }

    return (left << HALF_BITS) | right;
  }

  private long aesPrf(long value, int round) {
    // Build a 16-byte AES input block from value + round number
    byte[] block = new byte[16];
    block[0] = (byte) round;
    block[1] = (byte) (value & 0xFF);
    block[2] = (byte) ((value >> 8) & 0xFF);
    // remaining bytes are zero — acts as domain separation

    try {
      Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
      aes.init(Cipher.ENCRYPT_MODE, aesKey);
      byte[] encrypted = aes.doFinal(block);
      // Take the first 8 bytes as a long, then mask to half-width
      long result = 0;
      for (int i = 0; i < 8; i++) {
        result = (result << 8) | (encrypted[i] & 0xFF);
      }
      return result & HALF_MASK;
    } catch (Exception e) {
      throw new RuntimeException("AES PRF failed", e);
    }
  }
}
