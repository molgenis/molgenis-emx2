package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;

public class PetStoreLoader implements AvailableDataModels.DataModelLoader {

  public static final String CATEGORY = "Category";
  public static final String TAG = "Tag";
  public static final String NAME = "name";
  public static final String PET = "Pet";
  public static final String ORDER = "Order";
  public static final String USER = "User";
  public static final String CATEGORY_COLUMN = "category";
  public static final String STATUS = "status";
  public static final String WEIGHT = "weight";
  public static final String ORDER_ID = "orderId";
  public static final String QUANTITY = "quantity";
  public static final String PRICE = "price";
  public static final String COMPLETE = "complete";
  public static final String EMAIL = "email";
  public static final String PARENT = "parent";
  public static final String COLORS = "colors";
  public static final String SPECIES = "species";
  public static final String MAMMALS = "mammals";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    schema.create(table(CATEGORY).add(column(NAME).setPkey()));

    schema.create(
        table(PET)
            .add(column(NAME).setDescription("the name").setPkey())
            .add(column(CATEGORY_COLUMN).setType(REF).setRefTable(CATEGORY).setRequired(true))
            .add(column("photoUrls").setType(STRING_ARRAY))
            .add(
                column("details")
                    .setType(HEADING)
                    .setDescription(
                        "Details")) // add a layout element, for now html formatting not allowed
            .add(column(STATUS)) // todo enum: available, pending, sold
            .add(column("tags").setType(ONTOLOGY_ARRAY).setRefTable(TAG))
            .add(column(WEIGHT).setType(DECIMAL).setRequired(true))
            .setDescription("My pet store example table"));

    schema.create(
        table(ORDER)
            .add(column(ORDER_ID).setPkey())
            .add(column("pet").setType(REF).setRefTable(PET))
            .add(column(QUANTITY).setType(LONG).setValidation("{quantity} >= 1"))
            .add(column(PRICE).setType(DECIMAL).setValidation("{price} >= 1"))
            .add(column(COMPLETE).setType(BOOL)) // todo: default false
            .add(column(STATUS))); // todo enum: placed, approved, delivered

    // refBack
    schema
        .getMetadata()
        .getTableMetadata(PET)
        .add(column("orders").setType(REFBACK).setRefTable(ORDER).setRefBack("pet"));

    schema.create(
        table(USER)
            .add(column("username").setPkey())
            .add(column("firstName"))
            .add(column("lastName"))
            .add(column("picture").setType(FILE))
            .add(column(EMAIL).setType(ColumnType.EMAIL))
            .add(column("password")) // todo: password type
            .add(column("phone")) // todo: validation phone
            .add(column("userStatus").setType(INT))
            .add(column("pets").setType(REF_ARRAY).setRefTable(PET)));

    if (includeDemoData) {
      loadExampleData(schema);
    }
  }

  private void loadExampleData(Schema schema) {
    final String shopviewer = "shopviewer";
    final String shopmanager = "shopmanager";
    final String shopowner = "shopowner";

    // initual user
    schema.addMember(shopmanager, "Manager");
    schema.getDatabase().setUserPassword(shopmanager, shopmanager);

    schema.addMember(shopviewer, "Viewer");
    schema.getDatabase().setUserPassword(shopviewer, shopviewer);

    schema.addMember(shopowner, "Owner");
    schema.getDatabase().setUserPassword(shopowner, shopowner);

    schema.getTable(CATEGORY).insert(new Row().set(NAME, "cat"), new Row().set(NAME, "dog"));
    schema
        .getTable(TAG)
        .insert(
            new Row().set(NAME, COLORS),
            new Row().set(NAME, "red").set(PARENT, COLORS),
            new Row().set(NAME, "green").set(PARENT, COLORS),
            new Row().set(NAME, SPECIES),
            new Row().set(NAME, MAMMALS).set(PARENT, SPECIES),
            new Row().set(NAME, "carnivorous mammals").set(PARENT, MAMMALS),
            new Row().set(NAME, "herbivorous mammals").set(PARENT, MAMMALS),
            new Row().set(NAME, "birds").set(PARENT, SPECIES));

    schema
        .getTable(PET)
        .insert(
            new Row()
                .set(CATEGORY_COLUMN, "cat")
                .set("name", "pooky")
                .set(STATUS, "available")
                .set(WEIGHT, 9.4),
            new Row()
                .set(CATEGORY_COLUMN, "dog")
                .set("name", "spike")
                .set(STATUS, "sold")
                .set("tags", "red,green")
                .set(WEIGHT, 15.7));

    schema
        .getTable(ORDER)
        .insert(
            new Row()
                .set(ORDER_ID, "1")
                .set("pet", "pooky")
                .set(QUANTITY, 1l)
                .set(PRICE, 9.99)
                .set(COMPLETE, true)
                .set(STATUS, "delivered"),
            new Row()
                .set(ORDER_ID, "2")
                .set("pet", "spike")
                .set(PRICE, 14.99)
                .set(QUANTITY, 7l)
                .set(COMPLETE, false)
                .set(STATUS, "approved"));

    schema.getTable(USER).insert(new Row().set("username", "bofke").set("pets", "spike,pooky"));

    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
  }
}
