package org.molgenis.emx2.examples;

import org.molgenis.emx2.*;

import static org.molgenis.emx2.ColumnType.*;

public class PetStoreExample {

  public static final String CATEGORY = "Category";
  public static final String TAG = "Tag";
  public static final String NAME = "name";
  public static final String PET = "Pet";
  private static final String ORDER = "Order";
  private static final String USER = "User";
  private static final String CATEGORY_COLUMN = "category";
  private static final String STATUS = "status";
  private static final String WEIGHT = "weight";
  private static final String ORDER_ID = "orderId";
  private static final String QUANTITY = "quantity";
  private static final String PRICE = "price";
  private static final String COMPLETE = "complete";

  private PetStoreExample() {
    // hide public constructor
  }

  public static void create(SchemaMetadata schema) {

    TableMetadata categoryTable = schema.createTable(CATEGORY);
    categoryTable.addColumn(NAME).setPrimaryKey(true);

    TableMetadata tagTable = schema.createTable(TAG);
    tagTable.addColumn(NAME).setPrimaryKey(true);

    TableMetadata petTable = schema.createTable(PET);
    petTable.addColumn(NAME).setPrimaryKey(true);
    petTable.addRef(CATEGORY_COLUMN, CATEGORY).setNullable(true);
    petTable.addColumn("photoUrls", STRING_ARRAY).setNullable(true);
    petTable.addColumn(STATUS); // todo enum: available, pending, sold
    petTable.addRefArray("tags", TAG).setNullable(true);
    petTable.addColumn(WEIGHT, DECIMAL);

    TableMetadata orderTable = schema.createTable(ORDER);
    orderTable.addColumn(ORDER_ID).primaryKey();
    orderTable.addRef("pet", PET, NAME).setReverseReference("orders");
    orderTable.addColumn(QUANTITY, INT); // todo: validation >=1
    orderTable.addColumn(PRICE, DECIMAL); // todo: validation >=1
    orderTable.addColumn(COMPLETE, BOOL); // todo: default false
    orderTable.addColumn(STATUS); // todo enum: placed, approved, delivered

    TableMetadata userTable = schema.createTable(USER);
    userTable.addColumn("username").setPrimaryKey(true);
    userTable.addColumn("firstName").setNullable(true);
    userTable.addColumn("lastName").setNullable(true);
    userTable.addColumn("email").setNullable(true); // todo: validation email
    userTable.addColumn("password").setNullable(true); // todo: password type
    userTable.addColumn("phone").setNullable(true); // todo: validation phone
    userTable.addColumn("userStatus", INT).setNullable(true);
    userTable.addRefArray("pets", PET).setNullable(true);
  }

  public static void populate(Schema schema) {

    // initual user
    schema.addMember("shopmanager", "Manager");

    schema.getTable(CATEGORY).insert(new Row().set(NAME, "cat"), new Row().set(NAME, "dog"));
    schema.getTable(TAG).insert(new Row().set(NAME, "red"), new Row().set(NAME, "green"));

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
                .set(QUANTITY, 1)
                .set(PRICE, 9.99)
                .set(COMPLETE, true)
                .set(STATUS, "delivered"),
            new Row()
                .set(ORDER_ID, "2")
                .set("pet", "spike")
                .set(PRICE, 14.99)
                .set(QUANTITY, 7)
                .set(COMPLETE, false)
                .set(STATUS, "approved"));

    schema.getTable(USER).insert(new Row().set("username", "bofke").set("pets", "spike,pooky"));
  }
}
