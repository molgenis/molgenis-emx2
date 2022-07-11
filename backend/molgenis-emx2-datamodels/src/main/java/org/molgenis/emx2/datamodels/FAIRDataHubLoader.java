package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class FAIRDataHubLoader implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {

    // create Beacon v2 + FAIR Data Point schema (which will create tables in ontology schema)
    createSchema(schema, "fairdatahub/beaconv2/molgenis.csv");
    createSchema(schema, "fairdatahub/fairdatapoint/molgenis.csv");

    // load ontologies
    MolgenisIO.fromClasspathDirectory("fairdatahub/ontologies", schema, false);

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("fairdatahub/beaconv2/demodata", schema, false);
      MolgenisIO.fromClasspathDirectory("fairdatahub/fairdatapoint/demodata", schema, false);
    }
  }

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    FAIRDataHubLoader.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }
}
