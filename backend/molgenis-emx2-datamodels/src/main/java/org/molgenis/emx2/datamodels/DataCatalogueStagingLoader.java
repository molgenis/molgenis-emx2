package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class DataCatalogueStagingLoader implements AvailableLoadersEnum.DataModelLoader {

  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";

  @Override
  public void loadMetadata(Schema schema) {

    // depends on CatalogueOntologies schema, so we create that if missing
    Database db = schema.getDatabase();
    if (!db.hasSchema(CATALOGUE_ONTOLOGIES)) {
      db.createSchema(CATALOGUE_ONTOLOGIES);
    }

    // create the schema
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    this.getClass()
                        .getClassLoader()
                        .getResourceAsStream("datacatalogue/Catalogue_cdm/molgenis.csv"))));
    schema.migrate(metadata);

    // load ontologies
    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);
  }

  @Override
  public void loadExampleData(Schema schema) {
    // nothingyet?
  }
}
