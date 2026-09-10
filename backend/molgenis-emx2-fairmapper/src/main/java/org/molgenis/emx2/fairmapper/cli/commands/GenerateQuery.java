package org.molgenis.emx2.fairmapper.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.SqlSchema;
import org.molgenis.emx2.sql.SqlTable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "generate-query",
    description =
        """
                Generate a sparql query based on a given EMX2 schema and table. Select and where clauses are set up based on
                schema metadata and how the columns are anotated with Semantics
                """,
    mixinStandardHelpOptions = true)
public class GenerateQuery implements Runnable {

  @CommandLine.Parameters(
      index = "0",
      description = "Name of Molgenis schema that contains the desired table")
  private String schemaName;

  @CommandLine.Parameters(index = "1", description = "Name of table to generate a query for.)")
  private String tableName;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "File to write the generated query to.")
  private String output;

  @Override
  @SuppressWarnings("java:S106")
  public void run() {
    SqlDatabase database = new SqlDatabase(false);
    database.becomeAdmin();

    SqlSchema schema = database.getSchema(schemaName);
    if (schema == null) {
      throw new MolgenisException("No schema found for: " + schemaName);
    }

    SqlTable table = schema.getTable(tableName);
    if (table == null) {
      throw new MolgenisException("No table found for: " + schemaName);
    }
    String generate = new TableQueryGenerator().generate(table.getMetadata());

    if (output != null) {
      try {
        Files.write(Path.of(output), generate.getBytes());
      } catch (IOException e) {
        throw new MolgenisException("Unable to write to desired output file: " + output, e);
      }
    }

    System.out.println(generate);
  }
}
