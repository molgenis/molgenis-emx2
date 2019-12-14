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
    petTable.addRef("category", CATEGORY).setNullable(true);
    petTable.addColumn("photoUrls", STRING_ARRAY).setNullable(true);
    petTable.addColumn("status"); // todo enum: available, pending, sold
    petTable.addRefArray("tags", TAG).setNullable(true);
    petTable.addColumn("weight", DECIMAL);

    TableMetadata orderTable = schema.createTable(ORDER);
    orderTable.addColumn("orderId").primaryKey();
    orderTable.addRef("pet", PET, NAME);
    orderTable.addColumn("quantity", INT); // todo: validation >=1
    orderTable.addColumn("price", DECIMAL); // todo: validation >=1
    orderTable.addColumn("complete", BOOL); // todo: default false
    orderTable.addColumn("status"); // todo enum: placed, approved, delivered

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
                .set("category", "cat")
                .set("name", "pooky")
                .set("status", "available")
                .set("weight", 9.4),
            new Row()
                .set("category", "dog")
                .set("name", "spike")
                .set("status", "sold")
                .set("tags", "red,green")
                .set("weight", 15.7));

    schema
        .getTable(ORDER)
        .insert(
            new Row()
                .set("orderId", "1")
                .set("pet", "pooky")
                .set("quantity", 1)
                .set("price", 9.99)
                .set("complete", true)
                .set("status", "delivered"),
            new Row()
                .set("orderId", "2")
                .set("pet", "spike")
                .set("price", 14.99)
                .set("quantity", 7)
                .set("complete", false)
                .set("status", "approved"));

    schema.getTable(USER).insert(new Row().set("username", "bofke").set("pets", "spike,pooky"));
  }
}
