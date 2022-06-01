package org.molgenis.emx2.datamodels;

import java.io.InputStreamReader;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class Beaconv2Loader implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {

    // create catalogue schema (which will create tables in ontology schema)
    createSchema(schema, "beaconv2/molgenis.csv");

    // optionally, load demo data
    if (includeDemoData) {
      MolgenisIO.fromClasspathDirectory("beaconv2/GenomicVariations", schema, false);
    }
  }

  public static void createSchema(Schema schema, String path) {
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    Beaconv2Loader.class.getClassLoader().getResourceAsStream(path))));
    schema.migrate(metadata);
  }
}
