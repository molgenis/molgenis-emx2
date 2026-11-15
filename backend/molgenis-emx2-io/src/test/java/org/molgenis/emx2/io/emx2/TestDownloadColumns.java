package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestDownloadColumns {

  private static TableMetadata pet;
  private static TableMetadata dog;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(TestDownloadColumns.class.getSimpleName());
    schema.create(
        table("Owner", column("firstName").setPkey(), column("lastName").setPkey()),
        table("Category", column("name").setPkey()),
        table(
            "Pet",
            column("pet name").setPkey(),
            column("details").setType(HEADING),
            column("weight").setType(DECIMAL),
            column("category").setType(REF).setRefTable("Category"),
            column("owner").setType(REF).setRefTable("Owner"),
            column("photo").setType(FILE)));
    pet = schema.getTable("Pet").getMetadata();

    schema.create(
        table("Animal", column("id").setPkey(), column("species")),
        table("Dog").setInheritName("Animal").add(column("breed")));
    dog = schema.getTable("Dog").getMetadata();
  }

  @Test
  void givenNoColumns_thenAllButSystemColumns() {
    List<String> all = DownloadColumns.resolve(pet, List.of(), false);
    assertEquals(
        List.of(
            "pet name",
            "weight",
            "category",
            "owner.firstName",
            "owner.lastName",
            "photo",
            "photo_filename",
            "mg_draft"),
        all);
    assertEquals(all, DownloadColumns.resolve(pet, null, false));
  }

  @Test
  void givenIncludeSystemColumns_thenSystemColumnsIncluded() {
    assertTrue(DownloadColumns.resolve(pet, List.of(), true).contains("mg_insertedOn"));
  }

  @Test
  void givenColumns_thenOnlyThoseInRequestedOrder() {
    assertEquals(
        List.of("weight", "pet name"),
        DownloadColumns.resolve(pet, List.of("weight", "pet name"), false));
  }

  @Test
  void givenIdentifier_thenColumnFound() {
    assertEquals(List.of("pet name"), DownloadColumns.resolve(pet, List.of("petName"), false));
  }

  @Test
  void givenSingleKeyReference_thenOneColumn() {
    assertEquals(List.of("category"), DownloadColumns.resolve(pet, List.of("category"), false));
  }

  @Test
  void givenCompositeKeyReference_thenExpandedToItsKeyColumns() {
    assertEquals(
        List.of("owner.firstName", "owner.lastName"),
        DownloadColumns.resolve(pet, List.of("owner"), false));
  }

  @Test
  void givenFile_thenExpandedToFileAndFilename() {
    assertEquals(
        List.of("photo", "photo_filename"), DownloadColumns.resolve(pet, List.of("photo"), false));
  }

  @Test
  void givenDuplicateColumn_thenIncludedOnce() {
    assertEquals(
        List.of("weight"), DownloadColumns.resolve(pet, List.of("weight", "weight"), false));
  }

  @Test
  void givenExplicitSystemColumn_thenIncludedWithoutFlag() {
    assertEquals(
        List.of("weight", "mg_insertedOn"),
        DownloadColumns.resolve(pet, List.of("weight", "mg_insertedOn"), false));
  }

  @Test
  void givenUnknownOrHeadingColumn_thenError() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> DownloadColumns.resolve(pet, List.of("weight", "colour", "details"), false));
    assertTrue(exception.getMessage().contains("colour, details"));
  }

  @Test
  void givenParentColumnOnChildTable_thenIncluded() {
    assertEquals(
        List.of("breed", "species"),
        DownloadColumns.resolve(dog, List.of("breed", "species"), false));
  }

  @Test
  void givenCompositeKeyPart_thenOnlyThatPart() {
    assertEquals(
        List.of("owner.firstName", "weight"),
        DownloadColumns.resolve(pet, List.of("owner.firstName", "weight"), false));
  }

  @Test
  void givenDotSeparatedColumnThatIsNoKeyPart_thenError() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                DownloadColumns.resolve(
                    pet, List.of("weight", "owner.age", "category.name"), false));
    assertTrue(exception.getMessage().contains("dot-separated column(s) owner.age, category.name"));
  }
}
