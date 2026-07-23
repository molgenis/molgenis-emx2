package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelSchemaValidatorTest {

  private static final ModelSchemaValidator VALIDATOR = new ModelSchemaValidator();

  private static List<String> validate(String tableType) {
    return VALIDATOR.validate("name: Terms\ntableType: " + tableType + "\n", "Terms.yaml");
  }

  @Test
  void tableTypeAcceptsAnyCaseLikeTheParser() {
    // the parser lower-cases tableType, so file-mode validation must accept the same spellings
    assertTrue(validate("data").isEmpty(), "canonical lowercase must validate");
    assertTrue(
        validate("Ontology").isEmpty(), "capitalised spelling must validate like the parser");
    assertTrue(validate("DATA").isEmpty(), "upper-case spelling must validate like the parser");
    assertTrue(validate("Module").isEmpty(), "mixed-case spelling must validate like the parser");

    // an unknown tableType is still rejected
    assertFalse(validate("nonsense").isEmpty(), "an unknown tableType must still fail validation");
  }
}
