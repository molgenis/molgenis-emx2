package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;

import graphql.language.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.PermissionEvaluator;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

public class GraphqlTableFragmentGenerator {
  private final Schema schema;
  private final boolean rowShape;

  private GraphqlTableFragmentGenerator(Schema schema, boolean rowShape) {
    this.schema = schema;
    this.rowShape = rowShape;
  }

  public static String rowSelection(Schema schema, TableMetadata table) {
    GraphqlTableFragmentGenerator generator = new GraphqlTableFragmentGenerator(schema, true);
    return AstPrinter.printAstCompact(
        generator.buildFragment(table, 1, 1, table.getIdentifier()).getSelectionSet());
  }

  public static Map<String, String> generate(Schema schema) {
    GraphqlTableFragmentGenerator generator = new GraphqlTableFragmentGenerator(schema, false);
    Map<String, String> fragments = new LinkedHashMap<>();
    for (TableMetadata table : schema.getMetadata().getTables()) {
      for (int depth = 1; depth <= 3; depth++) {
        fragments.put(
            table.getIdentifier() + "AllFields" + depth, generator.toFragmentString(table, depth));
      }
      fragments.put(
          table.getIdentifier() + "AllFields",
          generator.toFragmentString(table, 1, table.getIdentifier() + "AllFields"));
    }
    return fragments;
  }

  private String toFragmentString(TableMetadata table, int depth) {
    String fragmentName = table.getIdentifier() + "AllFields" + depth;
    return AstPrinter.printAst(buildFragment(table, depth, depth, fragmentName)) + "\n";
  }

  private String toFragmentString(TableMetadata table, int depth, String fragmentName) {
    return AstPrinter.printAst(buildFragment(table, depth, depth, fragmentName)) + "\n";
  }

  private FragmentDefinition buildFragment(
      TableMetadata table, int depth, int maxDepth, String fragmentName) {
    String nestedFragmentName = depth == 0 ? table.getIdentifier() + "KeyFields" : fragmentName;
    List<Column> columns = depth == 0 ? table.getPrimaryKeyColumns() : table.getStoredColumns();
    if (rowShape) {
      columns = rowColumns(table, depth);
    }

    String typeName = getTableTypeName(table);
    List<Selection<?>> selections = new ArrayList<>();

    columns.forEach(
        column -> {
          if (column.isFile()) {
            List<Selection<?>> file = new ArrayList<>();
            file.add(Field.newField("size").build());
            file.add(Field.newField("id").build());
            file.add(Field.newField("filename").build());
            file.add(Field.newField("extension").build());
            if (rowShape) {
              file.add(Field.newField("url").build());
            }
            selections.add(
                Field.newField(column.getIdentifier())
                    .selectionSet(SelectionSet.newSelectionSet(file).build())
                    .build());
          } else if (column.isReference()) {
            int nextDepth = depth > 0 ? depth - 1 : 0;
            String nestedName = column.getRefTable().getIdentifier() + "Nested";
            selections.add(
                Field.newField(column.getIdentifier())
                    .selectionSet(
                        buildFragment(column.getRefTable(), nextDepth, maxDepth, nestedName)
                            .getSelectionSet())
                    .build());
          } else {
            selections.add(Field.newField(column.getIdentifier()).build());
          }
        });

    SelectionSet selectionSet = SelectionSet.newSelectionSet().selections(selections).build();

    return FragmentDefinition.newFragmentDefinition()
        .name(nestedFragmentName)
        .typeCondition(TypeName.newTypeName(typeName).build())
        .selectionSet(selectionSet)
        .build();
  }

  public static List<Column> rowColumns(TableMetadata table) {
    return table.getColumnsIncludingSubclasses().stream()
        .filter(
            column ->
                !column.isHeading()
                    && !column.isRefback()
                    && (!column.isSystemColumn() || MG_TABLECLASS.equals(column.getName())))
        .collect(
            Collectors.toMap(
                Column::getName, c -> c, (first, ignored) -> first, LinkedHashMap::new))
        .values()
        .stream()
        .toList();
  }

  private List<Column> rowColumns(TableMetadata table, int depth) {
    List<Column> columns = depth == 0 ? table.getPrimaryKeyColumns() : rowColumns(table);
    return columns.stream().filter(column -> !column.isReference() || canView(column)).toList();
  }

  private boolean canView(Column column) {
    return PermissionEvaluator.canView(schema, column.getRefTable());
  }

  private String getTableTypeName(TableMetadata table) {
    if (table.getSchemaName().equals(schema.getName())) {
      return table.getIdentifier();
    } else {
      return table.getSchema().getIdentifier().replace("-", "") + "_" + table.getIdentifier();
    }
  }
}
