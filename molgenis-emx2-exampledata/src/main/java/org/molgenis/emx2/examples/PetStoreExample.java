package org.molgenis.emx2.examples;

import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.Type.BOOL;
import static org.molgenis.emx2.Type.INT;
import static org.molgenis.emx2.Type.STRING_ARRAY;

public class PetStoreExample {

  private PetStoreExample() {
    // hide public constructor
  }

  public static void create(SchemaMetadata schema) throws MolgenisException {

    TableMetadata categoryTable = schema.createTableIfNotExists("Category");
    categoryTable.addColumn("name").setUnique(true);

    TableMetadata tagTable = schema.createTableIfNotExists("Tag");
    tagTable.addColumn("name").setUnique(true);

    TableMetadata petTable = schema.createTableIfNotExists("Pet");
    petTable.addColumn("name").setUnique(true);
    petTable.addRef("category", "Category").setNullable(true);
    petTable.addColumn("photoUrls", STRING_ARRAY);
    petTable.addColumn("status"); // todo enum: available, pending, sold
    petTable.addRefArray("tags", "Tag");

    TableMetadata userTable = schema.createTableIfNotExists("User");
    userTable.addColumn("username").setUnique(true);
    userTable.addColumn("firstName");
    userTable.addColumn("lastName");
    userTable.addColumn("email"); // todo: validation email
    userTable.addColumn("password"); // todo: password type?
    userTable.addColumn("phone"); // todo: validation phone
    userTable.addColumn("userStatus", INT);

    TableMetadata orderTable = schema.createTableIfNotExists("Order");
    orderTable.addRef("petId", "Pet", "name");
    orderTable.addColumn("quantity", INT); // todo: validation >=1
    orderTable.addColumn("complete", BOOL); // todo: default false
    orderTable.addColumn("status"); // todo enum: placed, approved, delivered
  }
}
