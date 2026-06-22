package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Reference} and the {@link Column#getReferences()} expansion that produces
 * them. These run fully in-memory (no database) so the value-object behaviour - flattening of
 * composite/array/refLink references and the {@link Reference} accessors - is pinned independently
 * of the SQL integration tests.
 */
public class ReferenceTest {

  /**
   * Resources(id) <- Contacts(resource->Resources, name) gives Contacts a composite primary key.
   */
  private static SchemaMetadata schemaWithCompositeKey() {
    SchemaMetadata schema = new SchemaMetadata("ReferenceTest");
    schema.create(table("Resources", column("id").setType(STRING).setKey(1)));
    schema.create(
        table(
            "Contacts",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("name").setType(STRING).setKey(1)));
    return schema;
  }

  private static Reference onlyReference(Column column) {
    List<Reference> refs = column.getReferences();
    assertEquals(1, refs.size(), "expected a single reference");
    return refs.get(0);
  }

  @Test
  void singleReferenceMapsToTargetPrimaryKey() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(table("SimpleRef", column("contact").setType(REF).setRefTable("Resources")));

    Reference ref = onlyReference(schema.getTableMetadata("SimpleRef").getColumn("contact"));

    assertAll(
        () -> assertEquals("contact", ref.getColumnName()),
        () -> assertEquals("id", ref.getReferencedColumnName()),
        () -> assertEquals("Resources", ref.getTargetTable()),
        () -> assertEquals("id", ref.getTargetColumn()),
        () -> assertEquals(STRING, ref.getPrimitiveType()),
        () -> assertEquals(REF, ref.getColumnType()),
        () -> assertFalse(ref.isArray()),
        () -> assertFalse(ref.isOverlapping()),
        () -> assertEquals(List.of("id"), ref.getPath()));
  }

  @Test
  void compositeReferenceExpandsIntoOnePartPerKeyColumn() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(table("CompositeRef", column("contact").setType(REF).setRefTable("Contacts")));

    List<Reference> refs =
        schema.getTableMetadata("CompositeRef").getColumn("contact").getReferences();

    assertEquals(2, refs.size());

    // First part follows the nested ref Contacts.resource -> Resources.id: the referenced column
    // (resource) is an intermediate target, distinct from the final target column (id).
    Reference resourcePart = refs.get(0);
    assertAll(
        () -> assertEquals("contact.resource", resourcePart.getColumnName()),
        () -> assertEquals("resource", resourcePart.getReferencedColumnName()),
        () -> assertEquals("Resources", resourcePart.getTargetTable()),
        () -> assertEquals("id", resourcePart.getTargetColumn()),
        () -> assertEquals(List.of("resource", "id"), resourcePart.getPath()));

    Reference namePart = refs.get(1);
    assertAll(
        () -> assertEquals("contact.name", namePart.getColumnName()),
        () -> assertEquals("name", namePart.getReferencedColumnName()),
        () -> assertEquals("Contacts", namePart.getTargetTable()),
        () -> assertEquals("name", namePart.getTargetColumn()),
        () -> assertEquals(List.of("name"), namePart.getPath()));
  }

  @Test
  void refArrayUsesArrayPrimitiveType() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(
        table("ArrayRef", column("contacts").setType(REF_ARRAY).setRefTable("Resources")));

    Reference ref = onlyReference(schema.getTableMetadata("ArrayRef").getColumn("contacts"));

    assertAll(
        () -> assertEquals("contacts", ref.getColumnName()),
        () -> assertEquals(REF_ARRAY, ref.getColumnType()),
        () -> assertEquals(STRING_ARRAY, ref.getPrimitiveType()),
        () -> assertTrue(ref.isArray()));
  }

  @Test
  void refLinkBorrowsNameAndIsMarkedOverlapping() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(
        table(
            "SubpopulationCounts",
            column("resource").setType(REF).setRefTable("Resources").setKey(1),
            column("subpopulation")
                .setType(REF)
                .setRefTable("Contacts")
                .setRefLink("resource")
                .setKey(1)));

    List<Reference> refs =
        schema.getTableMetadata("SubpopulationCounts").getColumn("subpopulation").getReferences();

    // The 'resource' part is shared with (borrowed from) the refLink column, so its name does not
    // start with 'subpopulation' and it is reported as overlapping.
    Reference borrowed =
        refs.stream().filter(r -> r.getColumnName().equals("resource")).findFirst().orElseThrow();
    assertTrue(borrowed.isOverlapping());

    Reference owned =
        refs.stream()
            .filter(r -> r.getColumnName().equals("subpopulation"))
            .findFirst()
            .orElseThrow();
    assertFalse(owned.isOverlapping());
  }

  @Test
  void withColumnNameReturnsImmutableCopy() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(table("SimpleRef", column("contact").setType(REF).setRefTable("Resources")));
    Reference ref = onlyReference(schema.getTableMetadata("SimpleRef").getColumn("contact"));

    Reference renamed = ref.withColumnName("renamed");

    assertAll(
        () -> assertNotSame(ref, renamed),
        () -> assertEquals("renamed", renamed.getColumnName()),
        () -> assertEquals("contact", ref.getColumnName(), "original must be unchanged"),
        // everything else carries over
        () -> assertEquals(ref.getReferencedColumnName(), renamed.getReferencedColumnName()),
        () -> assertEquals(ref.getTargetTable(), renamed.getTargetTable()),
        () -> assertEquals(ref.getTargetColumn(), renamed.getTargetColumn()),
        () -> assertEquals(ref.getPrimitiveType(), renamed.getPrimitiveType()),
        () -> assertEquals(ref.getColumnType(), renamed.getColumnType()),
        () -> assertEquals(ref.isArray(), renamed.isArray()),
        () -> assertEquals(ref.isRequired(), renamed.isRequired()),
        () -> assertEquals(ref.getPath(), renamed.getPath()));
  }

  @Test
  void toPrimitiveColumnUsesColumnNameAndPrimitiveType() {
    SchemaMetadata schema = schemaWithCompositeKey();
    schema.create(table("SimpleRef", column("contact").setType(REF).setRefTable("Resources")));
    Reference ref = onlyReference(schema.getTableMetadata("SimpleRef").getColumn("contact"));

    Column primitive = ref.toPrimitiveColumn();

    assertAll(
        () -> assertEquals("contact", primitive.getName()),
        () -> assertEquals(STRING, primitive.getColumnType()));
  }
}
