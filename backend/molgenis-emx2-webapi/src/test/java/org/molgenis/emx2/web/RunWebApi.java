package org.molgenis.emx2.web;

import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RunWebApi {

  public static void main(String[] args) throws IOException {

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    String url = "jdbc:postgresql:molgenis";
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");
    dataSource.setPassword("molgenis");

    // setup
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("pet store");
    PET_STORE.getImportTask(schema, true).run();

    MolgenisWebservice.start(8080);
  }
}
