package org.molgenis.emx2.fairmapper.postprocessing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class EmailPostProcessorTest {

  private static final String SCHEMA_NAME = EmailPostProcessorTest.class.getSimpleName();

  private InMemoryTableStore tableStore;
  private SchemaMetadata schema;

  @BeforeEach
  void setup() {
    schema = new SchemaMetadata(SCHEMA_NAME);

    schema.create(
        new TableMetadata("Contacts")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("email").setType(ColumnType.EMAIL),
                Column.column("alternativeEmails").setType(ColumnType.EMAIL_ARRAY),
                Column.column("name").setType(ColumnType.STRING),
                Column.column("homepage").setType(ColumnType.HYPERLINK)));

    schema.create(
        new TableMetadata("Organisations")
            .add(
                Column.column("id").setType(ColumnType.STRING).setPkey(),
                Column.column("email").setType(ColumnType.EMAIL)));

    tableStore = new InMemoryTableStore();
  }

  @Test
  void shouldRemoveEmailPrefix() {
    store("Contacts", new Row("id", "contact-1", "email", "mailto:BME.Seccbioimage@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    assertEquals("BME.Seccbioimage@tue.nl", contact().getString("email"));
  }

  @Test
  void shouldLeaveEmailWithoutPrefixUnchanged() {
    store("Contacts", new Row("id", "contact-1", "email", "BME.Seccbioimage@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    assertEquals("BME.Seccbioimage@tue.nl", contact().getString("email"));
  }

  @Test
  void shouldOnlyRemovePrefixFromTheStartOfTheValue() {
    store("Contacts", new Row("id", "contact-1", "email", "info+mailto:test@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    assertEquals("info+mailto:test@tue.nl", contact().getString("email"));
  }

  @Test
  void shouldHandleNullValue() {
    store("Contacts", new Row("id", "contact-1", "email", null));

    new EmailPostProcessor(schema).process(tableStore);

    assertNull(contact().getString("email"));
  }

  @Test
  void shouldHandleEmptyValue() {
    store("Contacts", new Row("id", "contact-1", "email", ""));

    new EmailPostProcessor(schema).process(tableStore);

    // Row.getString normalizes an empty value to null; the point here is that it survives
    // processing without failing and without gaining a value
    assertNull(contact().getString("email"));
  }

  @Test
  void shouldLeaveRowWithoutEmailColumnUntouched() {
    store("Contacts", new Row("id", "contact-1", "name", "John Doe"));

    new EmailPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(List.of(new Row("id", "contact-1", "name", "John Doe")), contacts());
  }

  @Test
  void shouldNotProcessColumnsOfOtherTypes() {
    store(
        "Contacts",
        new Row(
            "id",
            "contact-1",
            "name",
            "mailto:BME.Seccbioimage@tue.nl",
            "homepage",
            "mailto:BME.Seccbioimage@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    Row contact = contact();
    assertEquals("mailto:BME.Seccbioimage@tue.nl", contact.getString("name"));
    assertEquals("mailto:BME.Seccbioimage@tue.nl", contact.getString("homepage"));
  }

  @Test
  void shouldProcessEveryRowInTheTable() {
    store(
        "Contacts",
        new Row("id", "contact-1", "email", "mailto:one@tue.nl"),
        new Row("id", "contact-2", "email", "mailto:two@tue.nl"),
        new Row("id", "contact-3", "email", "three@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    CompareTools.assertEquals(
        List.of(
            new Row("id", "contact-1", "email", "one@tue.nl"),
            new Row("id", "contact-2", "email", "two@tue.nl"),
            new Row("id", "contact-3", "email", "three@tue.nl")),
        contacts());
  }

  @Test
  void shouldProcessEveryTableInTheStore() {
    store("Contacts", new Row("id", "contact-1", "email", "mailto:contact@tue.nl"));
    store("Organisations", new Row("id", "org-1", "email", "mailto:org@tue.nl"));

    new EmailPostProcessor(schema).process(tableStore);

    assertEquals("contact@tue.nl", contact().getString("email"));
    assertEquals("org@tue.nl", organisation().getString("email"));
  }

  @Nested
  class EmailArrayTest {

    @Test
    void shouldRemovePrefixFromEveryValue() {
      store(
          "Contacts",
          new Row(
              "id",
              "contact-1",
              "alternativeEmails",
              new String[] {"mailto:one@tue.nl", "mailto:two@tue.nl"}));

      new EmailPostProcessor(schema).process(tableStore);

      assertArrayEquals(
          new String[] {"one@tue.nl", "two@tue.nl"}, contact().getStringArray("alternativeEmails"));
    }

    @Test
    void shouldOnlyRemovePrefixFromPrefixedValues() {
      store(
          "Contacts",
          new Row(
              "id",
              "contact-1",
              "alternativeEmails",
              new String[] {"mailto:one@tue.nl", "two@tue.nl"}));

      new EmailPostProcessor(schema).process(tableStore);

      assertArrayEquals(
          new String[] {"one@tue.nl", "two@tue.nl"}, contact().getStringArray("alternativeEmails"));
    }

    @Test
    void shouldHandleNullValue() {
      store("Contacts", new Row("id", "contact-1", "alternativeEmails", null));

      new EmailPostProcessor(schema).process(tableStore);

      assertNull(contact().getStringArray("alternativeEmails"));
    }

    @Test
    void shouldHandleEmptyArray() {
      store("Contacts", new Row("id", "contact-1", "alternativeEmails", new String[] {}));

      new EmailPostProcessor(schema).process(tableStore);

      assertArrayEquals(new String[] {}, contact().getStringArray("alternativeEmails"));
    }
  }

  /**
   * When a table references another table whose (composite) key contains an email column, the
   * reference is flattened into columns like {@code contactPoint.email} that carry the same
   * unprocessed value and therefore need the same treatment.
   */
  @Nested
  class CompositeKeyTest {

    @BeforeEach
    void setup() {
      Database database = TestDatabaseFactory.getTestDatabase();
      schema = database.dropCreateSchema(CompositeKeyTest.class.getSimpleName()).getMetadata();

      schema.create(
          new TableMetadata("Contacts")
              .add(
                  Column.column("id").setType(ColumnType.STRING).setPkey(),
                  Column.column("email").setType(ColumnType.EMAIL).setPkey()));

      schema.create(
          new TableMetadata("Collections")
              .add(
                  Column.column("id").setType(ColumnType.STRING).setPkey(),
                  Column.column("contactPoint").setType(ColumnType.REF).setRefTable("Contacts"),
                  Column.column("contributors")
                      .setType(ColumnType.REF_ARRAY)
                      .setRefTable("Contacts")));
    }

    @Test
    void givenRef_whenCompositeKeyContainsEmail_thenProcess() {
      store(
          "Collections",
          new Row(
              "id",
              "col-1",
              "contactPoint.id",
              "contact-1",
              "contactPoint.email",
              "mailto:BME.Seccbioimage@tue.nl"));

      new EmailPostProcessor(schema).process(tableStore);

      Row collection = collection();
      assertEquals("contact-1", collection.getString("contactPoint.id"));
      assertEquals("BME.Seccbioimage@tue.nl", collection.getString("contactPoint.email"));
    }

    @Disabled("bugged because ref_arrays cannot recognize email cells at the moment")
    @Test
    void givenRefArray_whenCompositeKeyContainsEmail_thenProcessAllValues() {
      store(
          "Collections",
          new Row(
              "id",
              "col-1",
              "contributors.id",
              new String[] {"contact-1", "contact-2"},
              "contributors.email",
              new String[] {"mailto:one@tue.nl", "mailto:two@tue.nl"}));

      new EmailPostProcessor(schema).process(tableStore);

      Row collection = collection();
      assertArrayEquals(
          new String[] {"contact-1", "contact-2"}, collection.getStringArray("contributors.id"));
      assertArrayEquals(
          new String[] {"one@tue.nl", "two@tue.nl"},
          collection.getStringArray("contributors.email"));
    }

    @Test
    void givenRef_whenValueIsNull_thenLeaveNull() {
      store(
          "Collections",
          new Row("id", "col-1", "contactPoint.id", null, "contactPoint.email", null));

      new EmailPostProcessor(schema).process(tableStore);

      assertNull(collection().getString("contactPoint.email"));
    }

    @Test
    void shouldProcessBothTheReferencedTableAndTheReference() {
      store("Contacts", new Row("id", "contact-1", "email", "mailto:one@tue.nl"));
      store(
          "Collections",
          new Row(
              "id",
              "col-1",
              "contactPoint.id",
              "contact-1",
              "contactPoint.email",
              "mailto:one@tue.nl"));

      new EmailPostProcessor(schema).process(tableStore);

      assertEquals("one@tue.nl", contact().getString("email"));
      assertEquals("one@tue.nl", collection().getString("contactPoint.email"));
    }
  }

  /** Writes rows for a table, deriving the column header from the union of all row keys. */
  private void store(String tableName, Row... rows) {
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }
    tableStore.writeTable(tableName, List.copyOf(columnNames), List.of(rows));
  }

  private List<Row> contacts() {
    return toList(tableStore.readTable("Contacts"));
  }

  private Row contact() {
    return contacts().getFirst();
  }

  private Row organisation() {
    return toList(tableStore.readTable("Organisations")).getFirst();
  }

  private Row collection() {
    return toList(tableStore.readTable("Collections")).getFirst();
  }

  private static List<Row> toList(Iterable<Row> rows) {
    return StreamSupport.stream(rows.spliterator(), false).toList();
  }
}
