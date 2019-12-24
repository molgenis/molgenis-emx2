package org.molgenis.emx2.examples;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.SchemaMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class ProductComponentPartsExample {
  public static final String PART = "Part";
  public static final String COMPONENT = "Component";
  public static final String PRODUCT = "Product";
  public static final String WEIGHT = "weight";
  public static final String NAME = "name";
  public static final String PARTS = "parts";
  public static final String EXPLORER = "explorer";
  public static final String NAVIGATOR = "navigator";
  public static final String LOGIN = "login";
  public static final String FORMS = "forms";
  public static final String COMPONENTS = "components";

  private ProductComponentPartsExample() {
    // hide constructor
  }

  public static void create(SchemaMetadata schema) {

    schema.create(
        table(PART)
            .addColumn(column(NAME))
            .addColumn(column(WEIGHT).type(INT))
            .setPrimaryKey(NAME));

    schema.create(
        table(COMPONENT)
            .addColumn(column(NAME))
            .addColumn(column(PARTS).type(REF_ARRAY).refTable(PART).refColumn(NAME))
            .setPrimaryKey(NAME));

    schema.create(
        table(PRODUCT)
            .addColumn(column(NAME))
            .addColumn(column(COMPONENTS).type(REF_ARRAY).refTable(COMPONENT).refColumn(NAME))
            .setPrimaryKey(NAME));
  }

  public static void populate(Schema schema) {

    Table partTable = schema.getTable(PART);
    Row part1 = new Row().setString(NAME, FORMS).setInt(WEIGHT, 100);
    Row part2 = new Row().setString(NAME, LOGIN).setInt(WEIGHT, 50);
    partTable.insert(part1);
    partTable.insert(part2);

    Table componentTable = schema.getTable(COMPONENT);
    Row component1 = new Row().setString(NAME, EXPLORER).setRefArray(PARTS, FORMS, LOGIN);
    Row component2 = new Row().setString(NAME, NAVIGATOR).setRefArray(PARTS, LOGIN);
    componentTable.insert(component1);
    componentTable.insert(component2);

    Table productTable = schema.getTable(PRODUCT);
    Row product1 =
        new Row().setString(NAME, "molgenis").setRefArray(COMPONENTS, EXPLORER, NAVIGATOR);

    productTable.insert(product1);
  }
}
