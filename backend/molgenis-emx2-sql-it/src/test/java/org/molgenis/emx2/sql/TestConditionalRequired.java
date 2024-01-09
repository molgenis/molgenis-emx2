package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestConditionalRequired {

  @Test
  public void testConditionallyRequiredOnSingleFieldInt() {
    String expression = "age > 5";

    TableMetadata tableMetadata =
        table(
            "Test",
            new Column("age").setType(ColumnType.INT),
            new Column("status").setType(ColumnType.STRING).setRequired(expression));

    Row validRow = new Row("age", 4, "status", null);

    SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), validRow); // success

    Row invalidRow = validRow.set("age", 6);

    assertThrows(
        MolgenisException.class,
        () -> {
          SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), invalidRow);
        });
  }

  @Test
  public void testConditionallyRequiredOnSingleField() {
    String requiredExpression = "if(field_one) 'if field_one is provided field_two is required'";

    TableMetadata tableMetadata =
        table(
            "Test",
            new Column("field_one").setType(ColumnType.STRING),
            new Column("field_two").setType(ColumnType.STRING).setRequired(requiredExpression));
    Row validRow =
        new Row(
            "field_one", "provided",
            "field_two", "provided");

    SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), validRow); // success

    Row invalidRow = new Row("field_one", "provided", "field_two", null);

    Exception exception =
        assertThrows(
            MolgenisException.class,
            () -> {
              SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), invalidRow);
            });

    assertEquals(
        exception.getMessage(),
        "column 'field_two' is required: "
            + "if field_one is provided field_two is required in ROW(field_one='provided' field_two='null' )");
  }

  @Test
  public void testConditionallyRequiredOnMultipleFields() {
    String requiredExpression =
        "if(onMedication && "
            + "age > 10 &&"
            + "weight > 3 &&"
            + "species === 'cat') 'Medical status should be provided when an old fat cat is on drugs'";

    TableMetadata tableMetadata =
        table(
            "Pet",
            new Column("age").setType(ColumnType.INT),
            new Column("weight").setType(ColumnType.DECIMAL),
            new Column("species").setType(ColumnType.STRING),
            new Column("onMedication").setType(ColumnType.BOOL),
            new Column("medicalStatus").setType(ColumnType.STRING).setRequired(requiredExpression));

    Row invalidRow =
        new Row(
            "age", "12",
            "weight", "4",
            "species", "cat",
            "onMedication", true);

    Exception exception =
        assertThrows(
            MolgenisException.class,
            () -> {
              SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), invalidRow);
            });

    assertEquals(
        exception.getMessage(),
        "column 'medicalStatus' is required: "
            + "Medical status should be provided when an old fat cat is on drugs "
            + "in ROW(age='12' weight='4' species='cat' onMedication='true' )");

    Row validRow = invalidRow.set("medicalStatus", "old and fat");
    SqlTypeUtils.applyValidationAndComputed(tableMetadata.getColumns(), validRow); // success
  }
}
