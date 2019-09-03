package org.molgenis.emx2.web;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.utils.MolgenisException;

import static org.molgenis.emx2.Type.*;

public class TestWebApi {

  public static void main(String[] args) throws MolgenisException {
    Database db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    SchemaMetadata schema = db.createSchema("pet store").getMetadata();
    PetStoreExample.create(schema);

    WebApiFactory.createWebApi(db);
  }
}
