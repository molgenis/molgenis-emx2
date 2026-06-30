package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;

class TestUpsert {

  private static final Database database = TestDatabaseFactory.getTestDatabase();

  private static final String SCHEMA = TestUpsert.class.getSimpleName();
  private Schema schema;
  private Table person;

  @BeforeEach
  void setup() {
    schema = database.dropCreateSchema(SCHEMA);

    // createColumn some tables with contents
    person =
        schema.create(
            table("person")
                .add(column("ID").setType(INT).setPkey())
                .add(column("first_name"))
                .add(column("last_name")));
  }

  @Test
  void whenExistingRow_thenAddMissingColumns() {
    Row row =
        new Row().setInt("ID", 1).setString("first_name", "John").setString("last_name", "Doe");

    person.insert(row);
    List<Row> original = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);

    Row updatedRow = new Row().setInt("ID", 1).setString("last_name", "Bastien");

    person.save(updatedRow);
    List<Row> updatedRows = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);

    CompareTools.assertEquals(
        updatedRows, List.of(original.getFirst().setString("last_name", "Bastien")));
  }

  @Test
  void givenExistingRow_whenRefColumn_thenIncludeRefValue() {
    person.getMetadata().add(column("parent").setType(REF_ARRAY).setRefTable("person"));
    person = schema.getTable("person");

    person.insert(
        new Row()
            .setInt("ID", 1)
            .setString("first_name", "Homer")
            .setString("last_name", "Simpson"),
        new Row()
            .setInt("ID", 2)
            .setString("first_name", "Marge")
            .setString("last_name", "Simpson"),
        new Row()
            .setInt("ID", 3)
            .setString("first_name", "Lisa")
            .setString("last_name", "Simpson")
            .setRefArray("parent", 1));

    person.save(new Row().setInt("ID", 3).setRefArray("parent", 1, 2));

    List<Row> updatedRows =
        person.where(f("ID", Operator.EQUALS, 3)).retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(
        updatedRows,
        List.of(
            new Row()
                .setInt("ID", 3)
                .setString("first_name", "Lisa")
                .setString("last_name", "Simpson")
                .setRefArray("parent", 1, 2)));
  }

  @Test
  void givenExistingRow_whenTableInheritance_thenIncludeInheritedColumns() {
    schema.create(
        table("family")
            .add(column("family_ID").setType(INT).setPkey())
            .add(column("family_name").setType(STRING)));

    TableMetadata personMetaData = this.person.getMetadata();
    personMetaData.setInheritName("family");
    personMetaData.dropColumn("last_name");
    personMetaData.dropColumn("ID");

    person = schema.getTable("person");
    person.insert(
        new Row()
            .setInt("family_ID", 1)
            .setString("first_name", "John")
            .setString("family_name", "Doe"));

    Row updatedRow = new Row().setInt("family_ID", 1).setString("first_name", "Jane");

    person.save(updatedRow);
    List<Row> updatedRows = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);

    updatedRows.forEach(System.out::println);
    CompareTools.assertEquals(
        updatedRows,
        List.of(
            new Row()
                .setInt("family_ID", 1)
                .setString("first_name", "Jane")
                .setString("family_name", "Doe")));
  }

  @Test
  void givenNewRow_whenMissingColumn_thenUseNull() {
    person.insert(new Row().setInt("ID", 1).setString("first_name", "John"));

    List<Row> actual = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);

    CompareTools.assertEquals(
        actual,
        List.of(
            new Row()
                .setInt("ID", 1)
                .setString("first_name", "John")
                .setString("last_name", null)));
  }

  @Test
  void givenNewRow_whenMissingRefColumn_thenUseNull() {
    person.getMetadata().add(column("parent").setType(REF_ARRAY).setRefTable("person"));
    person = schema.getTable("person");

    person.save(
        new Row().setInt("ID", 1).setString("first_name", "John").setString("last_name", "Doe"));
    List<Row> actual = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);

    CompareTools.assertEquals(
        actual,
        List.of(
            new Row()
                .setInt("ID", 1)
                .setString("first_name", "John")
                .setString("last_name", "Doe")
                .setRefArray("parent", null)));
  }

  @Test
  void givenNewRow_whenInheritedColumn_thenUseNull() {
    schema.create(
        table("family")
            .add(column("family_ID").setType(INT).setPkey())
            .add(column("family_name").setType(STRING)));

    TableMetadata personMetaData = this.person.getMetadata();
    personMetaData.setInheritName("family");
    personMetaData.dropColumn("last_name");
    personMetaData.dropColumn("ID");

    person = schema.getTable("person");
    person.save(
        new Row()
            .setInt("family_ID", 1)
            .setString("first_name", "John")
            .setString("family_name", null));

    List<Row> updatedRows = person.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(
        updatedRows,
        List.of(
            new Row()
                .setInt("family_ID", 1)
                .setString("first_name", "John")
                .setString("family_name", null)));
  }
}
