package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class TableStoreForCsvInZipFileStreamingTest {

  private static final List<String> COLUMNS = List.of("id", "name");

  @Test
  void writeTableStreamingWritesHeaderAndRows() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(tmp.resolve("test.zip"));

      store.writeTableStreaming(
          "Pet",
          COLUMNS,
          consumer -> {
            for (int i = 0; i < 3; i++) {
              Row row = new Row();
              row.set("id", "id" + i);
              row.set("name", "name" + i);
              consumer.accept(row);
            }
          });

      List<Row> read = new ArrayList<>();
      store.readTable("Pet").forEach(read::add);
      assertEquals(3, read.size());
      assertEquals("id0", read.get(0).getString("id"));
      assertEquals("name2", read.get(2).getString("name"));
    } finally {
      deleteRecursively(tmp);
    }
  }

  @Test
  void writeTableStreamingWritesHeaderWhenThereAreNoRows() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(tmp.resolve("test.zip"));

      store.writeTableStreaming("Pet", COLUMNS, consumer -> {});

      assertTrue(store.containsTable("Pet"));
      List<Row> read = new ArrayList<>();
      store.readTable("Pet").forEach(read::add);
      assertEquals(0, read.size());
    } finally {
      deleteRecursively(tmp);
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    try (Stream<Path> files = Files.walk(dir)) {
      files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }
}
