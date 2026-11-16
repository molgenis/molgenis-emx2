package org.molgenis.emx2.io.tablestore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.conf.PlainParquetConfiguration;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.parquet.RowRecordConversion;
import org.molgenis.emx2.io.tablestore.processor.RowProcessor;

public class TableStoreForParquet implements TableStore {

  private static final PlainParquetConfiguration PARQUET_CONFIG = new PlainParquetConfiguration();
  private static final GenericData GENERIC_DATA = GenericData.get();

  private final Map<String, Path> tableFiles = new HashMap<>();

  public TableStoreForParquet(Map<String, Path> tableFiles) {
    this.tableFiles.putAll(tableFiles);
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    Path file = getMappedFile(name);
    Schema schema = schemaFor(columnNames);

    try (ParquetWriter<GenericRecord> writer = writerForFile(file, schema)) {
      for (Row row : rows) {
        GenericRecord record = new GenericData.Record(schema);
        for (int i = 0; i < columnNames.size(); i++) {
          record.put(i, row.getString(columnNames.get(i)));
        }
        writer.write(record);
      }
    } catch (IOException e) {
      throw new MolgenisException("Writing parquet file for table '" + name + "' failed", e);
    }
  }

  @Override
  public Iterable<Row> readTable(String name) {
    Path file = getMappedFile(name);
    List<Row> rows = new ArrayList<>();
    try (ParquetReader<GenericRecord> reader =
        AvroParquetReader.<GenericRecord>builder(new LocalInputFile(file), PARQUET_CONFIG)
            .withDataModel(GENERIC_DATA)
            .build()) {
      GenericRecord record;
      while ((record = reader.read()) != null) {
        rows.add(RowRecordConversion.toRow(record));
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
    Path file = tableFiles.get(name);
    return file != null && Files.exists(file);
  }

  @Override
  public Collection<String> getTableNames() {
    return tableFiles.keySet().stream().filter(this::containsTable).toList();
  }

  private static Schema schemaFor(List<String> columnNames) {
    Set<String> usedFieldNames = new HashSet<>();
    for (String columnName : columnNames) {
      if (columnName.equals(toValidFieldName(columnName))) {
        usedFieldNames.add(columnName);
      }
    }

    SchemaBuilder.FieldAssembler<Schema> fields = SchemaBuilder.record("Row").fields();
    for (String columnName : columnNames) {
      fields =
          fields
              .name(uniqueFieldName(columnName, usedFieldNames))
              .prop(RowRecordConversion.COLUMN_NAME_PROPERTY, columnName)
              .type()
              .optional()
              .stringType();
    }
    return fields.endRecord();
  }

  private static String uniqueFieldName(String columnName, Set<String> usedFieldNames) {
    String fieldName = toValidFieldName(columnName);
    if (fieldName.equals(columnName)) {
      return columnName;
    }

    String candidate = fieldName;
    int suffix = 2;
    while (usedFieldNames.contains(candidate)) {
      candidate = fieldName + "_" + suffix;
      suffix++;
    }
    usedFieldNames.add(candidate);
    return candidate;
  }

  private static String toValidFieldName(String columnName) {
    return columnName.replaceAll("[^A-Za-z0-9_]", "_");
  }

  private static ParquetWriter<GenericRecord> writerForFile(Path file, Schema schema)
      throws IOException {
    return AvroParquetWriter.<GenericRecord>builder(new LocalOutputFile(file))
        .withConf(PARQUET_CONFIG)
        .withSchema(schema)
        .withDataModel(GENERIC_DATA)
        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
        .build();
  }

  private Path getMappedFile(String name) {
    Path file = tableFiles.get(name);
    if (file == null) {
      throw new MolgenisException("No parquet file mapped for table '" + name + "'");
    }

    if (!Files.exists(file)) {
      throw new MolgenisException("Parquet file for table '" + name + "' not found: " + file);
    }
    return file;
  }
}
