package org.molgenis.emx2.email;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

class EmailValidatorTest {

  @Test
  void toInternetAddress() {
    assertThrows(
        MolgenisException.class,
        () -> EmailValidator.toInternetAddress("i am not a email address"));
    assertDoesNotThrow(() -> EmailValidator.toInternetAddress("test@molgenis.org"));
  }
}
