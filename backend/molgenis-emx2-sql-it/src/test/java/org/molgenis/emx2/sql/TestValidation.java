package org.molgenis.emx2.sql;

import static org.junit.Assert.fail;
import static org.molgenis.emx2.Row.row;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.PetStoreLoader;

public class TestValidation {

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestValidation.class.getSimpleName());
  }

  @Test
  public void testValidation() {
    new PetStoreLoader().loadMetadata(schema);
    new PetStoreLoader().loadExampleData(schema); // has some validations

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
}
