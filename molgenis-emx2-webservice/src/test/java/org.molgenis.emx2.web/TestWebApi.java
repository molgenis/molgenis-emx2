package org.molgenis.emx2.web;

import org.molgenis.Database;
import org.molgenis.SchemaMetadata;
import org.molgenis.TableMetadata;
import org.molgenis.sql.DatabaseFactory;
import org.molgenis.utils.MolgenisException;

import static org.molgenis.Type.*;

public class TestWebApi {

  public static void main(String[] args) throws MolgenisException {
    Database db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    SchemaMetadata schema = db.createSchema("pet store").getMetadata();

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
    userTable.addColumn("phone"); // todo: validation phon
    userTable.addColumn("userStatus", INT);

    TableMetadata orderTable = schema.createTableIfNotExists("Order");
    orderTable.addRef("petId", "Pet", "name");
    orderTable.addColumn("quantity", INT); // todo: validation >=1
    orderTable.addColumn("complete", BOOL); // todo: default false
    orderTable.addColumn("status"); // todo enum: placed, approved, delivered

    WebApiFactory.createWebApi(db);
  }
}
