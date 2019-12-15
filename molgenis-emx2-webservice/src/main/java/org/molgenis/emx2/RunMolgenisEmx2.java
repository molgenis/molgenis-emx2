package org.molgenis.emx2;

import com.zaxxer.hikari.HikariDataSource;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisWebservice;

public class RunMolgenisEmx2 {

  public static void main(String[] args) {

    String url = "jdbc:postgresql:molgenis";

    if (args.length == 1) {
      url = args[0];
      System.out.println("Starting EMX2 with postgresql url: " + url);
    } else {
      System.out.println("Warning: takes 1 argument with the jdbc url; otherwise uses " + url);
    }

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup
    Database db = new SqlDatabase(dataSource);
    if (db.getSchema("pet store") == null) {
      Schema schema = db.createSchema("pet store");
      PetStoreExample.create(schema.getMetadata());
      PetStoreExample.populate(schema);
    }

    // start
    MolgenisWebservice.start(dataSource);
  }
}
