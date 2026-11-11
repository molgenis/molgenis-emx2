package org.molgenis.emx2.email;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  @Test
  void isValidEmail() {
    assertTrue(EmailValidator.isValidEmail("test@molgenis.org"));
    assertFalse(EmailValidator.isValidEmail("i am not a email address"));
  }

  @Test
  void validationResponseToRecievers() {
    Map<String, Object> validationResponse = new LinkedHashMap<>();
    Map<String, Object> data = new LinkedHashMap<>();
    List<Map> resources = new ArrayList<>();
    Map resource = new LinkedHashMap<>();
    List<Map> contacts = new ArrayList<>();

    Map<String, Object> contactValue = new LinkedHashMap<>();

    validationResponse.put("data", data);
    data.put("Resources", resources);
    resources.add(resource);
    resource.put("contacts", contacts);
    contactValue.put("email", "test@molgenis.org");
    contacts.add(contactValue);

    List<String> expected = Collections.singletonList("test@molgenis.org");
    assertEquals(expected, EmailValidator.validationResponseToReceivers(validationResponse));
  }
}
