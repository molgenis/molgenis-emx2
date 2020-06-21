package org.molgenis.emx2;

import com.zaxxer.hikari.HikariDataSource;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisWebservice;

import java.io.IOException;
import java.util.regex.Pattern;

public class RunMolgenisEmx2 {

  public static void main(String[] args) throws IOException {
    String url = "jdbc:postgresql:molgenis";
    if (args.length == 1) {
      if (Pattern.matches("[0-9A-Za-z/:]+", args[0])) {
        url = args[0];
      } else {
        System.out.println("Error: invalid jdbc string. Found '" + args[0] + "'");
        return;
      }
    } else {
      System.out.println(
          "You can pass custom postgresql jdbc url as first parameter as commandline");
    }
    System.out.println(
        "Starting EMX2 Version=" + Version.getVersion() + " with postgresql url: " + url);

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup database
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
