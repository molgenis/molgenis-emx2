package org.molgenis.emx2;

import com.zaxxer.hikari.HikariDataSource;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisWebservice;

import java.net.MalformedURLException;

public class RunMolgenisEmx2 {

  public static void main(String[] args) {

    String defaultUrl = "jdbc:postgresql:molgenis";

    if (args.length == 1) {
      System.out.println("Starting EMX2 with postgresql url: " + args[0]);
    } else {
      System.out.println(
          "Warning: takes 1 argument with the jdbc url; otherwise uses " + defaultUrl);
    }

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(args[0] != null ? args[0] : defaultUrl);
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
