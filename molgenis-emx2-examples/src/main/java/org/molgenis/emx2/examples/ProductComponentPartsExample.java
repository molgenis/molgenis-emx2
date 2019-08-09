package org.molgenis.emx2.examples;

import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Schema;
import org.molgenis.Table;

import static org.molgenis.Type.INT;
import static org.molgenis.Type.STRING;

public class ProductComponentPartsExample {

  public static void create(Schema schema) throws MolgenisException {

    String PART = "Part";
    Table partTable = schema.createTableIfNotExists(PART);
    partTable.addColumn("name", STRING);
    partTable.addColumn("weight", INT);
    partTable.addUnique("name");

    Row part1 = new Row().setString("name", "forms").setInt("weight", 100);
    Row part2 = new Row().setString("name", "login").setInt("weight", 50);
    partTable.insert(part1);
    partTable.insert(part2);

    String COMPONENT = "Component";
    Table componentTable = schema.createTableIfNotExists(COMPONENT);
    componentTable.addColumn("name", STRING);
    componentTable.addUnique("name");
    componentTable.addRefArray("parts", "Part", "name");

    Row component1 = new Row().setString("name", "explorer").setRefArray("parts", "forms", "login");
    Row component2 = new Row().setString("name", "navigator").setRefArray("parts", "login");
    componentTable.insert(component1);
    componentTable.insert(component2);

    String PRODUCT = "Product";
    Table productTable = schema.createTableIfNotExists(PRODUCT);
    productTable.addColumn("name", STRING);
    productTable.addUnique("name");
    productTable.addRefArray("components", "Component", "name");

    Row product1 =
        new Row().setString("name", "molgenis").setRefArray("components", "explorer", "navigator");

    productTable.insert(product1);
  }
}
