package org.molgenis.emx2.web;

import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.RunMolgenisEmx2;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RunWebApi {

  public static void main(String[] args) {

    // setup
    Database db = TestDatabaseFactory.getTestDatabase();
    PET_STORE.getImportTask(db, "pet store", "", true).run();

    new MolgenisWebservice()
        .start(RunMolgenisEmx2.resolveHttpPort(args, RunMolgenisEmx2::environmentLookup));
  }
}
