package org.molgenis.emx2.utils.generator;

import java.security.SecureRandom;
import java.util.function.IntSupplier;

public class ConfiguringIdGenerator implements IdGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final AutoIdFormat.Format format;
  private final int length;
  private final IntSupplier supplier;

  ConfiguringIdGenerator(AutoIdFormat.Format format, int length, IntSupplier supplier) {
    this.format = format;
    this.length = length;
    this.supplier = supplier;
  }

  public static ConfiguringIdGenerator fromAutoIdConfig(AutoIdFormat config) {
    return new ConfiguringIdGenerator(
        config.format(),
        config.length(),
        () -> RANDOM.nextInt(config.format().getCharacters().length()));
  }

  @Override
  public String generateId() {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < length; i++) {
      builder.append(format.getCharacters().charAt(supplier.getAsInt()));
    }

    return builder.toString();
  }
}
