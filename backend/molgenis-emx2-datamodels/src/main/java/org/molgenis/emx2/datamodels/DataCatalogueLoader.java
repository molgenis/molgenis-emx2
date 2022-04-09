package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class DataCatalogueLoader implements AvailableLoadersEnum.DataModelLoader {

  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";

  @Override
  public void loadMetadata(Schema schema) {

    // depends on CatalogueOntologies schema, so we create that if missing
    Database db = schema.getDatabase();
    intitOntologies(db);
    createSchema(schema, "datacatalogue/molgenis.csv");
    loadOntologies(db);
  }

  public static void loadOntologies(Database db) {
    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);
  }

  public static void intitOntologies(Database db) {
    if (!db.hasSchema(CATALOGUE_ONTOLOGIES)) {
      db.createSchema(CATALOGUE_ONTOLOGIES);
    }
  }

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    DataCatalogueLoader.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }

  @Override
  public void loadExampleData(Schema schema) {
    // load example data
    MolgenisIO.fromClasspathDirectory("datacatalogue/Cohorts", schema, false);
  }
}
