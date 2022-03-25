package org.molgenis.emx2.examples;

import java.io.File;
import java.io.IOException;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogueExample {
  private CatalogueExample() {}

  public static void create(Database database) {

    Schema ontologySchema = database.dropCreateSchema("CatalogueOntologies");
    Schema catalogueSchema = database.dropCreateSchema("Catalogue");

    // load catalogue data model
    SchemaMetadata catalogueMetadata = null;
    try {
      catalogueMetadata =
          Emx2.fromRowList(CsvTableReader.read(new File("../../data/datacatalogue/molgenis.csv")));
    } catch (IOException e) {
      throw new MolgenisException("Failed to load molgenis.csv sample data", e);
    }

    ontologySchema.migrate(catalogueMetadata);

    // load ontologies data
    MolgenisIO.fromDirectory(
        new File("../../data/datacatalogue/CatalogueOntologies").toPath(), ontologySchema, false);

    // load schema data
    MolgenisIO.fromDirectory(
        new File("../../data/datacatalogue/Cohorts").toPath(), catalogueSchema, false);

    MolgenisIO.importFromExcelFile(
        new File("../../data/datacatalogue/Cohorts_CoreVariables.xlsx").toPath(),
        catalogueSchema,
        false);
  }
}
