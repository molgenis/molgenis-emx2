package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Stack;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ConfiguringIdGeneratorTest {

  @Test
  void givenLetters_thenGenerateUsingLetters() {
    ConfiguringIdGenerator generator =
        new ConfiguringIdGenerator(
            AutoIdConfig.Format.LETTERS, 5, createStaticSupplier(0, 51, 13, 34, 20));
    assertEquals("AzNiU", generator.generateId());
  }

  @Test
  void givenNumbers_thenGenerateUsingNumbers() {
    ConfiguringIdGenerator generator =
        new ConfiguringIdGenerator(
            AutoIdConfig.Format.NUMBERS, 5, createStaticSupplier(0, 3, 8, 2, 9));
    assertEquals("03829", generator.generateId());
  }

  @Test
  void givenMixed_thenGenerateUsingNumbersAndLetters() {
    ConfiguringIdGenerator generator =
        new ConfiguringIdGenerator(
            AutoIdConfig.Format.MIXED, 9, createStaticSupplier(0, 53, 55, 51, 13, 60, 34, 20, 58));
    assertEquals("A13zN8iU6", generator.generateId());
  }

  @Test
  void givenLength_thenGenerateSetLength() {
    ConfiguringIdGenerator generator =
        new ConfiguringIdGenerator(AutoIdConfig.Format.LETTERS, 1, createStaticSupplier(0));
    assertEquals("0", generator.generateId());
  }

  @Test
  void fromConfigurationFuzzTest() {
    Pattern pattern = Pattern.compile("\\d{5}");
    ConfiguringIdGenerator generator =
        ConfiguringIdGenerator.fromAutoIdConfig(new AutoIdConfig(AutoIdConfig.Format.NUMBERS, 5));

    for (int i = 0; i < 50; i++) {
      Matcher matcher = pattern.matcher(generator.generateId());
      assertTrue(matcher.matches());
    }
  }

  private static IntSupplier createStaticSupplier(int... values) {
    Stack<Integer> stack = new Stack<>();

    for (int i = values.length - 1; i >= 0; i--) {
      stack.push(values[i]);
    }

    return new IntSupplier() {
      final Stack<Integer> values = stack;

      @Override
      public int getAsInt() {
        return values.pop();
      }
    };
  }
}
