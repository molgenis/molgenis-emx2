package org.molgenis.emx2.io;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.rowstore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;

public class SchemaImport {
  private static Logger logger = LoggerFactory.getLogger(SchemaImport.class.getName());

  private SchemaImport() {
    // hide constructor
  }

  public static void fromDirectory(Path directory, Schema schema) {
    executeImport(new TableStoreForCsvFilesDirectory(directory), schema);
  }

  public static void fromZipFile(Path zipFile, Schema schema) {
    executeImport(new TableStoreForCsvInZipFile(zipFile), schema);
  }

  public static void fromExcelFile(Path excelFile, Schema schema) {
    executeImport(new TableStoreForXlsxFile(excelFile), schema);
  }

  static void executeImport(TableStore store, Schema schema) {
    long start = System.currentTimeMillis();
    schema.tx(
        db -> {
          // read emx1 metadata, if available (to be removed in future versions)
          if (store.containsTable("attributes")) {
            Emx1.uploadFromStoreToSchema(store, schema);
          } else if (store.containsTable("molgenis")) {
            SchemaMetadata emx2Schema = Emx2.fromRowList(store.readTable("molgenis"));
            schema.merge(emx2Schema);
          }
          // read data
          for (Table table : schema.getTablesSorted()) {
            if (store.containsTable(table.getName())) {

              // validation of fkeys
              // store.processTable(table.getName(), new ValidationProcessor(table));

              // batching here to not blow memory,
              // and in strategy class so reader can close filet store.processTable(table.getName(),
              // new ImportProcessor(table));

              logger.info("Import of table '" + table.getName() + "' completed");
            }
          }
        });
    logger.info("Import transaction completed in " + (System.currentTimeMillis() - start) + "ms");
  }

  /** validates foreign keys against table */
  private static class ValidationProcessor implements RowProcessor {
    private final Table table;

    public ValidationProcessor(Table table) {
      this.table = table;
    }

    @Override
    public void process(Iterator<Row> iterator) {
      long start = System.currentTimeMillis();
      logger.info("starting validation for " + table.getName());

      Map<String, Set> keys = new LinkedHashMap<>();
      List<Column> columns = table.getMetadata().getColumns();

      // find all unique refs
      int count = 0;
      while (iterator.hasNext()) {
        Row row = iterator.next();
        count++;
        for (Column c : columns) {
          // todo will fail on indirect circular refs; should make this optional?
          // instead, we should make trigger return list of errors instead of only first
          if (REF.equals(c.getColumnType())) {
            for (Reference ref : c.getReferences()) {
              if (keys.get(ref.getName()) == null) {
                keys.put(ref.getName(), new HashSet());
              }
              if (row.get(ref.getName(), ref.getColumnType()) != null) {
                keys.get(ref.getName()).add(row.get(ref.getName(), ref.getColumnType()));
              }
            }
          } else if (REF_ARRAY.equals(c.getColumnType())) {
            for (Reference ref : c.getReferences()) {
              if (keys.get(ref.getName()) == null) {
                keys.put(ref.getName(), new HashSet());
              }
              if (row.get(ref.getName(), ref.getColumnType()) != null) {
                for (Object value : (Object[]) row.get(ref.getName(), ref.getColumnType())) {
                  keys.get(ref.getName()).add(value);
                }
              }
            }
          }
        }
        if (count % 100000 == 0) {
          logger.info("Validating row " + count);
        }
      }

      logger.info(
          "indexed fkeys to check "
              + table.getName()
              + " in "
              + (System.currentTimeMillis() - start)
              + "ms");

      // then check each key
      for (Column c : columns) {
        if (REF.equals(c.getColumnType()) || REF_ARRAY.equals(c.getColumnType())) {
          for (Reference ref : c.getReferences()) {
            List<Row> result =
                table
                    .getSchema()
                    .getTable(c.getRefTableName())
                    .query()
                    .where(f(ref.getTo(), Operator.EQUALS, keys.get(ref.getName()).toArray()))
                    .retrieveRows();
            for (Row r : result) {
              keys.get(ref.getName())
                  .remove(
                      r.get(ref.getTo(), c.getRefTable().getColumn(ref.getTo()).getColumnType()));
            }
          }
        }
      }
      // if any of the lists non-empty we have missing
      String result = "";
      for (Column c : table.getMetadata().getColumns()) {
        if (REF.equals(c.getColumnType()) || REF_ARRAY.equals(c.getColumnType())) {
          for (Reference ref : c.getReferences()) {
            if (keys.get(ref.getName()).size() > 0) {
              result +=
                  "Keys missing for column "
                      + ref.getName()
                      + ": "
                      + keys.get(ref.getName()).stream().collect(Collectors.joining(","))
                      + ". ";
            }
          }
        }
      }
      logger.info(
          "validation for "
              + table.getName()
              + " took "
              + (System.currentTimeMillis() - start)
              + "ms");
      if (result.length() > 0) {
        throw new MolgenisException("Import of table " + table.getName() + " failed: " + result);
      }
    }
  }

  /** executes the import */
  private static class ImportProcessor implements RowProcessor {
    private final Table table;

    public ImportProcessor(Table table) {
      this.table = table;
    }

    @Override
    public void process(Iterator<Row> iterator) {
      int count = 0;
      List<Row> batch = new ArrayList<>();
      while (iterator.hasNext()) {
        batch.add(iterator.next());
        count++;
        if (batch.size() >= 100000) {
          table.update(batch);
          batch.clear();
          logger.info("Imported " + count + " into " + table.getName());
        }
      }
      // remaining
      if (batch.size() > 0) {
        table.update(batch);
        logger.info("Imported " + count + " into " + table.getName());
      }
    }
  }
}
