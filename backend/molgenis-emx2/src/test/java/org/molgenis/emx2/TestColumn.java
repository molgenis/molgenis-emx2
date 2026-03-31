package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.Test;

public class TestColumn {

  private static SchemaMetadata createResourcesContactsSchema() {
    SchemaMetadata schema = new SchemaMetadata("TestColumn");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Contacts",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("name").setType(STRING).setKey(1)));
    return schema;
  }

  @Test
  public void refLabelDefaultExcludesRefBackColumn() {
    SchemaMetadata schema = createResourcesContactsSchema();
    schema
        .getTableMetadata("Resources")
        .add(column("contacts").setType(REFBACK).setRefTable("Contacts").setRefBack("resource"));

    Column contactsColumn = schema.getTableMetadata("Resources").getColumn("contacts");
    assertEquals("${name}", contactsColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultExcludesBackRefForRefColumn() {
    SchemaMetadata schema = createResourcesContactsSchema();
    schema
        .getTableMetadata("Resources")
        .add(column("contact").setType(REF).setRefTable("Contacts"));

    Column contactColumn = schema.getTableMetadata("Resources").getColumn("contact");
    assertEquals("${name}", contactColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultExcludesBackRefForRefArrayColumn() {
    SchemaMetadata schema = createResourcesContactsSchema();
    schema
        .getTableMetadata("Resources")
        .add(column("contacts").setType(REF_ARRAY).setRefTable("Contacts"));

    Column contactsColumn = schema.getTableMetadata("Resources").getColumn("contacts");
    assertEquals("${name}", contactsColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultIncludesAllPartsForUnrelatedTable() {
    SchemaMetadata schema = createResourcesContactsSchema();
    schema.create(table("Other", column("id").setType(STRING).setKey(1)));
    schema.getTableMetadata("Other").add(column("contact").setType(REF).setRefTable("Contacts"));

    Column contactColumn = schema.getTableMetadata("Other").getColumn("contact");
    assertEquals("${resource.id} ${name}", contactColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultRefbackIndirectViaIntermediary() {
    SchemaMetadata schema = new SchemaMetadata("TestColumn");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Datasets",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("name").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Variables",
            column("dataset").setType(REF).setRefTable("Datasets").setKey(1),
            column("name").setType(STRING).setKey(1)));
    schema
        .getTableMetadata("Resources")
        .add(column("variables").setType(REFBACK).setRefTable("Variables").setRefBack("dataset"));

    Column variablesColumn = schema.getTableMetadata("Resources").getColumn("variables");
    assertEquals("${name}", variablesColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultRefIndirectNoOverlap() {
    SchemaMetadata schema = new SchemaMetadata("TestColumn");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Datasets",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("name").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Variables",
            column("dataset").setType(REF).setRefTable("Datasets").setKey(1),
            column("name").setType(STRING).setKey(1)));
    schema
        .getTableMetadata("Resources")
        .add(column("variable").setType(REF).setRefTable("Variables"));

    Column variableColumn = schema.getTableMetadata("Resources").getColumn("variable");
    assertEquals(
        "${dataset.resource.id} ${dataset.name} ${name}", variableColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultRefbackMultipleFksSkipsOnlyRefBackColumn() {
    SchemaMetadata schema = new SchemaMetadata("TestColumn");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Linkages",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("linkedResource").setType(REF).setRefTable("Resources").setKey(1)));
    schema
        .getTableMetadata("Resources")
        .add(column("linkages").setType(REFBACK).setRefTable("Linkages").setRefBack("resource"));

    Column linkagesColumn = schema.getTableMetadata("Resources").getColumn("linkages");
    assertEquals("${linkedResource.id}", linkagesColumn.getRefLabelDefault());
  }

  @Test
  public void refLabelDefaultWithRefLink() {
    SchemaMetadata schema = new SchemaMetadata("TestColumn");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Subpopulations",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("name").setType(STRING).setKey(1)));
    schema.create(
        table(
            "SubpopulationCounts",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("subpopulation")
                .setType(REF)
                .setRefTable("Subpopulations")
                .setRefLink("resource")
                .setKey(1),
            column("ageGroup").setType(STRING).setKey(1)));
    schema
        .getTableMetadata("Resources")
        .add(
            column("counts")
                .setType(REFBACK)
                .setRefTable("SubpopulationCounts")
                .setRefBack("resource"));

    Column countsColumn = schema.getTableMetadata("Resources").getColumn("counts");
    assertEquals("${subpopulation.name} ${ageGroup}", countsColumn.getRefLabelDefault());
  }

  @Test
  public void isSystemColumn() {
    assertAll(
        () -> assertFalse(new Column("test").isSystemColumn()),
        () -> assertTrue(new Column("mg_test").isSystemColumn()),
        () -> assertFalse(new Column("MG_test").isSystemColumn()));
  }

  @Test
  public void validColumnName() {
    assertAll(
        // valid: 1 or more legal characters
        () -> assertDoesNotThrow(() -> new Column("a")),
        // valid: a space
        () -> assertDoesNotThrow(() -> new Column("first name")),
        // valid: space & underscore but not next to each other
        () -> assertDoesNotThrow(() -> new Column("yet_another name")),
        // invalid: # should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("#first name")),
        // invalid: '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first_  name")),
        // invalid: ' _' not allowed
        () -> assertThrows(MolgenisException.class, () -> new Column("first   _name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  _  name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("first  __   name")),
        // invalid: ' _' & '_ ' should fail
        () -> assertThrows(MolgenisException.class, () -> new Column("aa    ____      ")),
        // valid: max length (= 63 characters) -> psql limit (as of 2024-09-03):
        // https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
        // https://www.postgresql.org/docs/current/runtime-config-preset.html#GUC-MAX-IDENTIFIER-LENGTH
        () ->
            assertDoesNotThrow(
                () ->
                    new Column("a23456789012345678901234567890123456789012345678901234567890123")),
        // invalid: too long (> 63 characters)
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Column(
                        "a234567890123456789012345678901234567890123456789012345678901234")));
  }
}
