package org.molgenis.emx2.typescript;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;

public class AToolToGenerateTypeScriptTypes {

  public static void main(String[] args) {
    System.out.println("Generating TypeScript types");

    if (args.length == 2) {
      String schemaName = args[0];
      System.out.println("  Generate from Schema: " + schemaName);

      String fileFullPath = args[1];
      System.out.println("  Generate into file: " + fileFullPath);

      generate(schemaName, fileFullPath);
      System.out.println("--- TypeScript type Generation completed ---");
    } else {
      System.out.println(
          "Missing required arguments ( SchemaName ( example: 'Pet Store'), full-file-path (example: '/home/bob/app/types.ts')");
    }
  }

  public static void generate(String schemaName, String fileFullPath) {
    SqlDatabase db = new SqlDatabase(SqlDatabase.ADMIN_USER, false);
    db.getJooq();
    Schema schema = db.getSchema(schemaName);
    if (schema == null) {
      System.out.println("Schema " + schemaName + " not found");
    }

    Generator generator = new Generator();
    generator.generate(schema, fileFullPath);
  }
}
