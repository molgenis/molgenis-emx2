package org.molgenis.emx2.io.tablestore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.parquet.RowParquetReaderBuilder;
import org.molgenis.emx2.io.parquet.RowParquetWriterBuilder;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

/**
 * The TableStore contains a schema that is only used when writing: it decides the Parquet type of
 * each column. So a table can only be written when the schema has a table with that name, and every
 * written column must be a column of that table. Trying to write tables that aren't part of the
 * schema, result in a {@link MolgenisException}.
 */
public class TableStoreForParquet implements TableStore {

  private static final String PARQUET_EXTENSION = ".parquet";
  private static final PlainParquetConfiguration PARQUET_CONFIG = new PlainParquetConfiguration();

  private final Path directoryPath;
  private final SchemaMetadata schema;

  public TableStoreForParquet(Path directoryPath, SchemaMetadata schema) {
    if (!Files.isDirectory(directoryPath)) {
      throw new MolgenisException("Directory " + directoryPath + " doesn't exist");
    }
    this.directoryPath = directoryPath;
    this.schema = schema;
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    writeTableStreaming(name, columnNames, rows::forEach);
  }

  @Override
  public void writeTableStreaming(String name, List<String> columnNames, RowProducer rows) {
    TableMetadata tableMetadata = schema.getTableMetadata(name);
    if (tableMetadata == null) {
      throw new MolgenisException("No table in schema " + schema.getName() + ": " + name);
    }

    RowParquetWriterBuilder builder =
        new RowParquetWriterBuilder(
                new LocalOutputFile(tableFile(name)), tableMetadata, columnNames)
            .withConf(PARQUET_CONFIG)
            .withWriteMode(ParquetFileWriter.Mode.OVERWRITE);

    try (ParquetWriter<Row> writer = builder.build()) {
      rows.produce(
          row -> {
            try {
              writer.write(row);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (IOException | UncheckedIOException e) {
      throw new MolgenisException("Writing parquet file for table '" + name + "' failed", e);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    Path file = tableFile(name);
    if (!Files.exists(file)) {
      throw new MolgenisException("Parquet file for table '" + name + "' not found: " + file);
    }

    List<Row> rows = new ArrayList<>();
    RowParquetReaderBuilder builder =
        new RowParquetReaderBuilder(new LocalInputFile(file), PARQUET_CONFIG);

    try (ParquetReader<Row> reader = builder.build()) {
      Row row;
      while ((row = reader.read()) != null) {
        rows.add(row);
      }
    } catch (IOException e) {
      throw new MolgenisException("Reading parquet file for table '" + name + "' failed", e);
    }
    return rows;
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    return Files.exists(tableFile(name));
  }

  @Override
  public Collection<String> getTableNames() {
    try (Stream<Path> files = Files.list(directoryPath)) {
      return files
          .filter(Files::isRegularFile)
          .map(file -> file.getFileName().toString())
          .filter(fileName -> fileName.endsWith(PARQUET_EXTENSION))
          .map(fileName -> fileName.substring(0, fileName.length() - PARQUET_EXTENSION.length()))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new MolgenisException("Listing parquet files in " + directoryPath + " failed", e);
    }
  }

  private Path tableFile(String name) {
    return directoryPath.resolve(name + PARQUET_EXTENSION);
  }
}
