package org.molgenis.emx2.io.parquet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.LocalInputFile;
import org.junit.jupiter.api.Test;

/**
 * Hadoop's client runtime is left out on purpose to reduce JAR size. This means only uncompressed
 * Parquet files can be read. This test fails once compressed files become readable, so that the
 * limitation is revisited rather than silently lifted.
 */
class CompressedParquetReadTest {

  @Test
  void givenSnappyCompressedFile_whenReadingRows_thenFails()
      throws IOException, URISyntaxException {
    Path file =
        Path.of(
            Objects.requireNonNull(getClass().getResource("/parquet/compressed.parquet")).toURI());

    ParquetReadOptions options =
        ParquetReadOptions.builder(new PlainParquetConfiguration()).build();

    try (ParquetFileReader reader = ParquetFileReader.open(new LocalInputFile(file), options)) {
      assertThrows(NoClassDefFoundError.class, reader::readNextRowGroup);
    }
  }
}
