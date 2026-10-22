package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;

class ColumnDependenciesTest {

  @Test
  void sortsDependenciesBeforeDependents() {
    List<Column> columns =
        List.of(
            column("a").setComputed("c + b"),
            column("b").setComputed("c"),
            column("c"),
            column("filler"));

    assertEquals(
        List.of("c", "b", "a", "filler"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void sortsOnIdentifierNotOnName() {
    List<Column> columns =
        List.of(
            column("other names").setVisible("nameComputed == 'Piet'"),
            column("filler"),
            column("name computed").setComputed("name"),
            column("name"));

    assertEquals(
        List.of("name", "name computed", "other names", "filler"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void keepsDeclarationOrderWhenThereAreNoDependencies() {
    List<Column> columns = List.of(column("a"), column("b"), column("c"));

    assertEquals(
        List.of("a", "b", "c"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void doesNotDependOnColumnThatIsOnlyMentionedInAStringLiteral() {
    List<Column> columns =
        List.of(column("a").setVisible("status == 'awaiting name'"), column("name"));

    assertEquals(
        List.of("a", "name"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void doesNotDependOnColumnThatIsOnlyAPropertyOfAnotherColumn() {
    List<Column> columns = List.of(column("a").setComputed("myRef.name"), column("name"));

    assertEquals(
        List.of("a", "name"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void doesNotDependOnColumnThatIsOnlyPartOfALargerIdentifier() {
    List<Column> columns =
        List.of(column("a").setVisible("nameComputed == 'Piet'"), column("name"));

    assertEquals(
        List.of("a", "name"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void doesNotDependOnColumnThatIsShadowedByALambdaParameter() {
    List<Column> columns =
        List.of(
            column("a").setComputed("myList.map(name => name.value)"),
            column("name").setComputed("a"));

    assertEquals(
        List.of("a", "name"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void dependsOnColumnThatIsOnlyShadowedInsideALambda() {
    List<Column> columns =
        List.of(
            column("a").setComputed("name + myList.map(name => name.value).length"),
            column("name"));

    assertEquals(
        List.of("name", "a"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void sortsMultiWordColumnNamesOnTheirIdentifier() {
    List<Column> columns =
        List.of(
            column("full name").setComputed("`${firstName} ${lastName}`"),
            column("last name"),
            column("filler"),
            column("first name"));

    assertEquals(
        List.of("last name", "first name", "full name", "filler"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }

  @Test
  void sortsColumnsInheritedFromASuperclassBeforeTheColumnsThatUseThem() {
    SchemaMetadata schema = new SchemaMetadata("my schema");
    schema.create(
        table("parent table", column("parent name")),
        table(
                "child table",
                column("child greeting").setComputed("parentName + ' ' + childSuffix"),
                column("child suffix"))
            .setInheritName("parent table"));

    List<Column> columns = schema.getTableMetadata("child table").getColumns();

    assertEquals(
        List.of("parent name", "child suffix", "child greeting"),
        ColumnDependencies.sortByDependencies(columns).stream().map(Column::getName).toList());
  }
}
