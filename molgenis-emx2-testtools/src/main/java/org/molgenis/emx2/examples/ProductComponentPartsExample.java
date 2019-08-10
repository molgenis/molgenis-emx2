package org.molgenis.emx2.examples;

import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.Schema;
import org.molgenis.Table;

import static org.molgenis.Type.INT;
import static org.molgenis.Type.STRING;

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

  public static void create(Schema schema) throws MolgenisException {

    Table partTable = schema.createTableIfNotExists(PART);
    partTable.addColumn(NAME, STRING);
    partTable.addColumn(WEIGHT, INT);
    partTable.addUnique(NAME);

    Table componentTable = schema.createTableIfNotExists(COMPONENT);
    componentTable.addColumn(NAME, STRING);
    componentTable.addUnique(NAME);
    componentTable.addRefArray(PARTS, PART, NAME);

    Table productTable = schema.createTableIfNotExists(PRODUCT);
    productTable.addColumn(NAME, STRING);
    productTable.addUnique(NAME);
    productTable.addRefArray(COMPONENTS, COMPONENT, NAME);
  }
}
