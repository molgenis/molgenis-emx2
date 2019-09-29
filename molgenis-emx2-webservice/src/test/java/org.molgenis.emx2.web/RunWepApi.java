package org.molgenis.emx2.web;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.utils.MolgenisException;

public class RunWepApi {

  public static void main(String[] args) throws MolgenisException {
    Database db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    Schema schema = db.createSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    MolgenisWebservice.start(db);
  }
}
