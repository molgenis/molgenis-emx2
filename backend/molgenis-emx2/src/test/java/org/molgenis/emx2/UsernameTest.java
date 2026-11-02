package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UsernameTest {

  @Test
  void givenSurroundingWhitespace_thenTrim() {
    assertEquals("hello", new Username("  hello  ").value());
  }

  @Test
  void givenTabsAndNewlines_thenTrim() {
    assertEquals("hello", new Username("\thello\n").value());
  }

  @Test
  void givenInnerWhitespace_thenKeep() {
    assertEquals("hello world", new Username("hello world").value());
  }

  private static Stream<Arguments> invalidUsernameValues() {
    return Stream.of(
        Arguments.of(null, "Username cannot be null"),
        Arguments.of("", "Username cannot be blank"),
        Arguments.of("    ", "Username cannot be blank"));
  }

  @ParameterizedTest
  @MethodSource("invalidUsernameValues")
  void givenOnlyWhitespaceInConstructor_thenThrow(String input, String expectedMessage) {
    MolgenisException exception = assertThrows(MolgenisException.class, () -> new Username(input));

    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  void givenDifferentValues_thenNotEquals() {
    assertNotEquals(new Username("hello"), new Username("world"));
  }
}
