package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Row.row;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestValidation {

  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestValidation.class.getSimpleName());
    new PetStoreLoader().load(schema, true);
  }

  @Test
  public void testValidation() throws IOException {
    // system level validation using email data type (will also test hyperlink indirectly)
    Table users = schema.getTable("User");
    try {
      users.insert(row("username", "john", "email", "wrong"));
      fail("email should fail on validation");
    } catch (Exception e) {
      // correct
    }
    users.insert(row("username", "john", "email", "correct@home.nl"));

    // now with some error

    // price can be null
    Table orders = schema.getTable("Order");
    orders.insert(row("orderId", "test"));

    // price cannot be 0
    try {
      orders.insert(row("orderId", "test2", "price", 0));
      fail("should validate on price > 0");
    } catch (Exception e) {
      // correcct
    }

    // also not on update
    // price cannot be 0
    try {
      orders.update(row("orderId", "test", "price", 0));
      fail("should validate on price > 0");
    } catch (Exception e) {
      // correct
    }
  }

  @Test
  public void testInvisibleRefTypeFieldsCanBeSaved() {
    // see https://github.com/molgenis/molgenis-emx2/issues/2331
    TableMetadata pet = schema.getTable("Pet").getMetadata();
    pet.alterColumn("tags", pet.getColumn("tags").setVisible("false").setRequired(true));
    pet.getTable()
        .insert(
            row(
                "name",
                "mickey",
                "category",
                "mouse",
                "weight",
                3)); // this should not fail on required tags
  }

  @Test
  public void testRequiredHeadingDoesntBlock() {
    // see https://github.com/molgenis/molgenis-emx2/issues/2384
    TableMetadata pet = schema.getTable("Pet").getMetadata();
    pet.alterColumn("details", pet.getColumn("details").setRequired(true));
    pet.getTable()
        .insert(
            row(
                "name",
                "mickey2",
                "category",
                "mouse",
                "weight",
                3)); // this should not fail on required details
  }
}
