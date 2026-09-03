package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.sql.ColumnDependencies.getExpressionVariables;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;

class ColumnDependenciesTest {

  @Test
  void findsVariablesOfComputed() {
    assertEquals(Set.of("b"), getExpressionVariables(column("a").setComputed("b + 1")));
  }

  @Test
  void findsVariablesOfRequired() {
    assertEquals(Set.of("b"), getExpressionVariables(column("a").setRequired("b != null")));
  }

  @Test
  void findsVariablesOfValidation() {
    assertEquals(Set.of("a", "b"), getExpressionVariables(column("a").setValidation("a > b")));
  }

  @Test
  void findsVariablesOfVisible() {
    assertEquals(Set.of("b"), getExpressionVariables(column("a").setVisible("b == 'yes'")));
  }

  @Test
  void findsVariablesOfComputedDefaultValue() {
    assertEquals(Set.of("b"), getExpressionVariables(column("a").setDefaultValue("=b * 2")));
  }

  @Test
  void findsVariablesOfObjectDefaultValueOfRef() {
    Column column =
        column("a", ColumnType.REF).setDefaultValue("={name: someColumn, code: otherColumn}");
    assertEquals(Set.of("someColumn", "otherColumn"), getExpressionVariables(column));
  }

  @Test
  void findsVariablesOfArrayDefaultValueOfRefArray() {
    Column column =
        column("a", ColumnType.REF_ARRAY).setDefaultValue("=[{name: someColumn, code: other}]");
    assertEquals(Set.of("someColumn", "other"), getExpressionVariables(column));
  }

  @Test
  void ignoresDefaultValueThatIsALiteral() {
    assertEquals(Set.of(), getExpressionVariables(column("a").setDefaultValue("b")));
  }

  @Test
  void ignoresVariablesDeclaredInTheExpressionItself() {
    Column column = column("a").setVisible("myList.filter(name => name.length > 0)");
    assertEquals(Set.of("myList"), getExpressionVariables(column));
  }

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
}
