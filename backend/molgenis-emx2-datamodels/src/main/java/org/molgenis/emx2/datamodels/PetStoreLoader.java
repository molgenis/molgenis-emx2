package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;

public class PetStoreLoader extends AbstractDataLoader {

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

  public SchemaMetadata getSchemaMetadata() {
    SchemaMetadata schema = new SchemaMetadata();
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
            .add(column(ORDER_ID).setPkey().setType(AUTO_ID).setComputed("ORDER:${mg_autoid}"))
            .add(column("pet").setType(REF).setRefTable(PET))
            .add(
                column(QUANTITY)
                    .setType(LONG)
                    .setValidation("if(quantity < 1) 'quantity should be >= 1'"))
            .add(column(PRICE).setType(DECIMAL).setValidation("price >= 1"))
            .add(column(COMPLETE).setType(BOOL)) // todo: default false
            .add(column(STATUS))); // todo enum: placed, approved, delivered

    // refBack
    schema
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

    return schema;
  }

  @Override
  void loadInternalImplementation(Schema schema, boolean includeDemoData) {
    schema.migrate(getSchemaMetadata());
    if (includeDemoData) {
      loadExampleData(schema);
    }
  }

  private void loadExampleData(Schema schema) {
    final String shopviewer = "shopviewer";
    final String shopmanager = "shopmanager";
    final String shopowner = "shopowner";

    // initialize users
    if (!schema.getDatabase().hasUser(shopmanager)) {
      schema.getDatabase().setUserPassword(shopmanager, shopmanager);
    }
    if (!schema.getDatabase().hasUser(shopviewer)) {
      schema.getDatabase().setUserPassword(shopviewer, shopviewer);
    }
    if (!schema.getDatabase().hasUser(shopowner)) {
      schema.getDatabase().setUserPassword(shopowner, shopowner);
    }
    schema.addMember(shopmanager, "Manager");
    schema.addMember(shopviewer, "Viewer");
    schema.addMember(shopowner, "Owner");

    schema
        .getTable(CATEGORY)
        .insert(
            new Row().set(NAME, "cat"),
            new Row().set(NAME, "dog"),
            new Row().set(NAME, "mouse"),
            new Row().set(NAME, "bird"),
            new Row().set(NAME, "ant"),
            new Row().set(NAME, "caterpillar"));
    schema
        .getTable(TAG)
        .insert(
            new Row().set(NAME, COLORS),
            new Row().set(NAME, "red").set(PARENT, COLORS),
            new Row().set(NAME, "green").set(PARENT, COLORS),
            new Row().set(NAME, "blue").set(PARENT, COLORS),
            new Row().set(NAME, "purple").set(PARENT, COLORS),
            new Row().set(NAME, SPECIES),
            new Row().set(NAME, MAMMALS).set(PARENT, SPECIES),
            new Row().set(NAME, "carnivorous mammals").set(PARENT, MAMMALS),
            new Row().set(NAME, "herbivorous mammals").set(PARENT, MAMMALS),
            new Row().set(NAME, "birds").set(PARENT, SPECIES),
            new Row().set(NAME, "insect").set(PARENT, SPECIES));

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
                .set(WEIGHT, 15.7),
            new Row()
                .set(CATEGORY_COLUMN, "cat")
                .set("name", "tom")
                .set("tags", "red")
                .set(STATUS, "available")
                .set(WEIGHT, 3.14),
            new Row()
                .set(CATEGORY_COLUMN, "cat")
                .set("name", "sylvester")
                .set("tags", "purple")
                .set(SPECIES, "carnivorous mammals")
                .set(STATUS, "available")
                .set(WEIGHT, 1.337),
            new Row()
                .set(CATEGORY_COLUMN, "mouse")
                .set("name", "jerry")
                .set("tags", "blue")
                .set(STATUS, "available")
                .set(WEIGHT, 0.18),
            new Row()
                .set(CATEGORY_COLUMN, "bird")
                .set("name", "tweety")
                .set("tags", "red")
                .set(STATUS, "available")
                .set(WEIGHT, 0.1),
            new Row()
                .set(CATEGORY_COLUMN, "caterpillar")
                .set("name", "the very hungry caterpillar")
                .set(STATUS, "available")
                .set(SPECIES, "insect")
                .set("tags", "green")
                .set(WEIGHT, 0.5),
            new Row()
                .set(CATEGORY_COLUMN, "ant")
                .set("name", "fire ant")
                .set(STATUS, "available")
                .set(SPECIES, "insect")
                .set("tags", "purple,red,green")
                .set(WEIGHT, 0.01));

    schema
        .getTable(ORDER)
        .insert(
            new Row()
                .set("pet", "pooky")
                .set(QUANTITY, 1l)
                .set(PRICE, 9.99)
                .set(COMPLETE, true)
                .set(STATUS, "delivered"),
            new Row()
                .set("pet", "spike")
                .set(PRICE, 14.99)
                .set(QUANTITY, 7l)
                .set(COMPLETE, false)
                .set(STATUS, "approved"));

    schema
        .getTable(USER)
        .insert(
            new Row()
                .set("username", "bofke")
                .set("pets", "spike,pooky,the very hungry caterpillar,fire ant"));

    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    schema
        .getMetadata()
        .setSetting(
            "reports",
            "[{\"id\":0,\"name\":\"pet report\",\"sql\":\"select * from \\\"Pet\\\"\"},{\"id\":1,\"name\":\"pet report with parameters\",\"sql\":\"select * from \\\"Pet\\\" p where p.name=ANY(${name:string_array})\"}]");
  }
}
