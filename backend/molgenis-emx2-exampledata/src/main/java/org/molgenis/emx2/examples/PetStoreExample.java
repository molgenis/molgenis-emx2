package org.molgenis.emx2.examples;

import org.molgenis.emx2.*;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class PetStoreExample {

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

  private PetStoreExample() {
    // hide public constructor
  }

  public static void create(SchemaMetadata schema) {

    schema.create(table(CATEGORY).addColumn(column(NAME)).setPrimaryKey(NAME));

    schema.create(table(TAG).addColumn(column(NAME)).setPrimaryKey(NAME));

    schema.create(
        table(PET)
            .addColumn(column(NAME).setDescription("the name"))
            .addColumn(column(CATEGORY_COLUMN).type(REF).refTable(CATEGORY))
            .addColumn(column("photoUrls").type(STRING_ARRAY).nullable(true))
            .addColumn(column(STATUS)) // todo enum: available, pending, sold
            .addColumn(column("tags").type(REF_ARRAY).refTable(TAG).nullable(true))
            .addColumn(column(WEIGHT).type(DECIMAL))
            .setPrimaryKey(NAME)
            .setDescription("My pet store example table"));

    schema.create(
        table(ORDER)
            .addColumn(column(ORDER_ID))
            .addColumn(column("pet").type(REF).refTable(PET).nullable(true))
            .addColumn(
                column(QUANTITY)
                    .type(INT)
                    .validation("if(value<1)'Must be larger than 1'")) // todo: validation >=1
            .addColumn(
                column(PRICE)
                    .type(DECIMAL)
                    .validation("if(value<1.0)'Must be larger than 1.0'")) // todo: validation >=1
            .addColumn(column(COMPLETE).type(BOOL)) // todo: default false
            .addColumn(column(STATUS))
            .setPrimaryKey(ORDER_ID)); // todo enum: placed, approved, delivered

    // refback
    schema
        .getTableMetadata(PET)
        .addColumn(column("orders").type(REFBACK).refTable(ORDER).mappedBy("pet"));

    schema.create(
        table(USER)
            .addColumn(column("username"))
            .addColumn(column("firstName").nullable(true))
            .addColumn(column("lastName").nullable(true))
            .addColumn(
                column(EMAIL)
                    .nullable(true)
                    .validation(
                        "if(!/^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,3})+$/.test(value)) 'Should be valid email address'")) // todo: validation email
            .addColumn(column("password").nullable(true)) // todo: password type
            .addColumn(column("phone").nullable(true)) // todo: validation phone
            .addColumn(column("userStatus").type(INT).nullable(true))
            .addColumn(column("pets").type(REF_ARRAY).refTable(PET).nullable(true))
            .setPrimaryKey("username"));
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
