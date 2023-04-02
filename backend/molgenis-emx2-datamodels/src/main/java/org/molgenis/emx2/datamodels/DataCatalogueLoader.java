package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.sql.SqlDatabase;

public class DataCatalogueLoader implements AvailableDataModels.DataModelLoader {

  public static final String CATALOGUE_ONTOLOGIES = "CatalogueOntologies";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    // create ontology schema
    Database db = schema.getDatabase();
    Schema ontologySchema = db.getSchema(CATALOGUE_ONTOLOGIES);
    if (ontologySchema == null) {
      ontologySchema = db.createSchema(CATALOGUE_ONTOLOGIES);
      ontologySchema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
    }

    // create catalogue schema (which will create tables in ontology schema)
    createSchema(schema, "datacatalogue/molgenis.csv");
    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());

    // load data into ontology schema
    MolgenisIO.fromClasspathDirectory("datacatalogue/CatalogueOntologies", ontologySchema, false);

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("datacatalogue/Cohorts", schema, false);
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
}
