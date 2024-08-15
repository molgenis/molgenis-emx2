package org.molgenis.emx2;

import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.typescript.Generator;

public class AToolToGenerateTypeScriptTypes {

  public static void main(String[] args) {
    generate();
  }

  public static void generate() {
    SqlDatabase db = new SqlDatabase(false);
    db.getJooq();
    db.becomeAdmin();

    Schema schema = db.dropCreateSchema("Generator-PetStore");

    PetStoreLoader petStoreLoader = new PetStoreLoader();
    petStoreLoader.load(schema, true);
    Generator generator = new Generator();
    generator.generate(schema, "bla");
  }
}
