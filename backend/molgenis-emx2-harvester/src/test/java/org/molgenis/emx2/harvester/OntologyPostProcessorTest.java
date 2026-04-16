package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.harvester.util.HarvestingTestSchema;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;

class OntologyPostProcessorTest {

  @Test
  void shouldReplaceOntologySemanticsWithNames() throws IOException {
    Path path = Path.of(OntologyPostProcessor.class.getResource("csv/Resources.csv").getPath());
    TableStore store = new TableStoreForCsvFile(path);
    SchemaMetadata schema = HarvestingTestSchema.create();

    TableMetadata resourcesTable = schema.getTableMetadata("Resources");
    OntologyPostProcessor processor = new OntologyPostProcessor(store, resourcesTable);
    processor.process();

    StringWriter writer = new StringWriter();
    List<String> columns =
        resourcesTable.getDownloadColumnNames().stream().map(Column::getName).toList();
    CsvTableWriter.write(store.readTable("Resources"), columns, writer, ',');

    System.out.println("CSV");
    System.out.println(writer);
    Files.write(Path.of("ontology_resources.csv"), writer.toString().getBytes());
    List<String> rowCountries =
        StreamSupport.stream(store.readTable("Resources").spliterator(), false)
            .map(row -> row.getString("countries"))
            .toList();
    assertTrue(rowCountries.contains("Denmark,Finland,Germany"));
  }
}
