package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestColumnCrossSchemaRefMissing {

  private static final String MISSING_REF_TABLE = "DoesNotExist";
  private static final String CONTACTS = "Contacts";
  private static final String UNRESOLVABLE_REF_SCHEMA =
      TestColumnCrossSchemaRefMissing.class.getSimpleName() + "Unresolvable";

  private static Database database;
  private static Schema referencingSchema;
  private static Schema referencedSchema;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    referencingSchema =
        database.dropCreateSchema(TestColumnCrossSchemaRefMissing.class.getSimpleName() + "a");
    referencedSchema =
        database.dropCreateSchema(TestColumnCrossSchemaRefMissing.class.getSimpleName() + "b");
    referencingSchema.create(table(CONTACTS).add(column("name").setPkey()));
  }

  @Test
  void throwsNamingReferencedSchemaAndTable() {
    Column crossSchemaRef =
        new Column(
            referencingSchema.getMetadata().getTableMetadata(CONTACTS),
            column("resource")
                .setType(REF)
                .setRefSchemaName(referencedSchema.getName())
                .setRefTable(MISSING_REF_TABLE));

    MolgenisException exception =
        assertThrows(MolgenisException.class, crossSchemaRef::getRefTable);
    assertAll(
        () ->
            assertTrue(exception.getMessage().contains(MISSING_REF_TABLE), exception.getMessage()),
        () ->
            assertTrue(
                exception.getMessage().contains(referencedSchema.getName()),
                exception.getMessage()),
        () ->
            assertFalse(
                exception.getMessage().contains("schema '" + referencingSchema.getName() + "'"),
                exception.getMessage()));
  }

  @Test
  void getReferenceRefbackPropagatesWhenRefSchemaUnresolvable() {
    Column unresolvableSchemaRef =
        new Column(
            referencingSchema.getMetadata().getTableMetadata(CONTACTS),
            column("resource")
                .setType(REF)
                .setRefSchemaName(UNRESOLVABLE_REF_SCHEMA)
                .setRefTable(MISSING_REF_TABLE));

    MolgenisException exception =
        assertThrows(MolgenisException.class, unresolvableSchemaRef::getReferenceRefback);
    assertTrue(exception.getMessage().contains(UNRESOLVABLE_REF_SCHEMA), exception.getMessage());
  }
}
