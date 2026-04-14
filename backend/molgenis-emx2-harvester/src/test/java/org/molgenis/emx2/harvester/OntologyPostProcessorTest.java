package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class OntologyPostProcessorTest {

  @Test
  void shouldReplaceOntologySemanticsWithNames() throws IOException {
    Path path = Path.of(OntologyPostProcessor.class.getResource("csv/resources.csv").getPath());
    TableStore store = new TableStoreForCsvFile(path);
    Database database = TestDatabaseFactory.getTestDatabase();
    SchemaMetadata schema = database.getSchema("catalogue").getMetadata();

    OntologyPostProcessor processor =
        new OntologyPostProcessor(store, schema.getTableMetadata("Resources"));
    processor.process();

    StringWriter writer = new StringWriter();
    List<String> columns =
        schema.getTableMetadata("Resources").getDownloadColumnNames().stream()
            .map(Column::getName)
            .toList();
    CsvTableWriter.write(store.readTable("Resources"), columns, writer, ',');

    System.out.println("CSV");
    System.out.println(writer);
    Files.write(Path.of("Resources.csv"), writer.toString().getBytes());
  }
}
