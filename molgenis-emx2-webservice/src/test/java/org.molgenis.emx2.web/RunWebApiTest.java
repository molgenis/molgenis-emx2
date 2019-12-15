package org.molgenis.emx2.web;

import com.zaxxer.hikari.HikariDataSource;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.MolgenisException;

public class RunWebApiTest {

  public static void main(String[] args) {

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    String url = "jdbc:postgresql:molgenis";
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup
    Database db = DatabaseFactory.getTestDatabase();
    Schema schema = db.createSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    MolgenisWebservice.start(dataSource);
  }
}
