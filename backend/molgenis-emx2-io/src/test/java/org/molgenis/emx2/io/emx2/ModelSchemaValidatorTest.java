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

  private static List<String> validateColumnType(String columnType) {
    return VALIDATOR.validate(
        "name: Person\ncolumns:\n  - name: firstName\n    type: " + columnType + "\n",
        "Person.yaml");
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

  @Test
  void columnTypeAcceptsAnyCaseLikeTheParser() {
    // the parser upper-cases column type before ColumnType.valueOf, so file-mode validation must
    // accept the same spellings
    assertTrue(validateColumnType("string").isEmpty(), "canonical lowercase must validate");
    assertTrue(
        validateColumnType("String").isEmpty(),
        "capitalised spelling must validate like the parser");
    assertTrue(
        validateColumnType("STRING").isEmpty(),
        "upper-case spelling must validate like the parser");
    assertTrue(
        validateColumnType("Ref_Array").isEmpty(),
        "mixed-case spelling must validate like the parser");

    // an unknown column type is still rejected
    assertFalse(
        validateColumnType("nonsense").isEmpty(),
        "an unknown column type must still fail validation");
  }
}
