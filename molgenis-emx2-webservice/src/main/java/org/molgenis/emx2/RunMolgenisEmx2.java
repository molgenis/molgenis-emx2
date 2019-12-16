package org.molgenis.emx2;

import com.zaxxer.hikari.HikariDataSource;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisWebservice;

public class RunMolgenisEmx2 {

  public static void main(String[] args) {

    String url = "jdbc:postgresql:molgenis";
    if (args.length == 1) {
      if (args[0].startsWith("jdbc:postgresql:") && args[0].endsWith("/molgenis")) {
        url = args[0];
      } else {
        System.out.println("Error: invalid jdbc string.");
        return;
      }
    } else {
      System.out.println("You can pass custom postgresql jdbc url as first parameter");
    }
    System.out.println("Starting EMX2 with postgresql url: " + url);

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
