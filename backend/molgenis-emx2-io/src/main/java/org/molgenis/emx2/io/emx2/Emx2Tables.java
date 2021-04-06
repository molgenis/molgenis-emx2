package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.*;
import org.jooq.Field;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.RowProcessor;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Emx2Tables {
  private static final Logger logger = LoggerFactory.getLogger(Emx2Tables.class.getName());

  private Emx2Tables() {
    // hidden
  }

  public static void outputTable(TableStore store, Table table) {
    SelectColumn[] select =
        table.getMetadata().getDownloadColumnNames().stream()
            .map(c -> c.getName())
            .filter(n -> !n.equals(MG_TABLECLASS))
            .map(c -> s(c))
            .toArray(SelectColumn[]::new);

    if (table.getMetadata().getColumnNames().contains(MG_TABLECLASS)) {
      store.writeTable(
          table.getName(),
          table
              .query()
              .select(select)
              .where(
                  f(
                      MG_TABLECLASS,
                      Operator.EQUALS,
                      table.getSchema().getName() + "." + table.getName()))
              .retrieveRows());
    } else {
      store.writeTable(table.getName(), table.select(select).retrieveRows());
    }

    // in case of zip file we include the attached files
    if (store instanceof TableStoreForCsvInZipFile) {
      Emx2Files.outputFiles((TableStoreForCsvInZipFile) store, table);
    }
  }

  public static void inputTable(TableStore store, Table table) {
    if (store.containsTable(table.getName())) {

      // validation of primary keys
      store.processTable(table.getName(), new PkeyValidator(table.getMetadata()));

      // validation of fkeys
      // store.processTable(table.getName(), new ValidationProcessor(table));

      // batching here to not blow memory,
      // and in strategy class so reader can close file
      store.processTable(table.getName(), new ImportProcessor(table));

      logger.info("Import of table '" + table.getName() + "' completed");
    }
  }

  public static class PkeyValidator implements RowProcessor {

    Set<String> duplicates = new HashSet<>();
    Set<String> keys = new HashSet<>();
    TableMetadata metadata;

    public PkeyValidator(TableMetadata metadata) {
      this.metadata = metadata;
    }

    @Override
    public void process(Iterator<Row> iterator) {
      while (iterator.hasNext()) {
        Row row = iterator.next();
        String key = null;
        for (Field f : metadata.getPrimaryKeyFields()) {
          key += row.getString(f.getName()) + "+";
        }
        key = key.substring(0, key.length() - 1);
        if (keys.contains(key)) {
          duplicates.add(key);
          logger.warn("Found duplicate key: " + key);
        } else {
          keys.add(key);
        }
      }
      if (duplicates.size() > 0) {
        throw new MolgenisException(
            "Duplicate keys found in table " + metadata.getTableName() + ": " + duplicates);
      }
    }
  }

  /** validates foreign keys against table */
  //  private static class ValidationProcessor implements RowProcessor {
  //    private final Table table;
  //
  //    public ValidationProcessor(Table table) {
  //      this.table = table;
  //    }
  //
  //    @Override
  //    public void process(Iterator<Row> iterator) {
  //      long start = System.currentTimeMillis();
  //      logger.info("starting validation for " + table.getName());
  //
  //      Map<String, Set> keys = new LinkedHashMap<>();
  //      List<Column> columns = table.getMetadata().getColumns();
  //
  //      // find all unique refs
  //      int count = 0;
  //      while (iterator.hasNext()) {
  //        Row row = iterator.next();
  //        count++;
  //        for (Column c : columns) {
  //          // todo will fail on indirect circular refs; should make this optional?
  //          // instead, we should make trigger return list of errors instead of only first
  //          if (REF.equals(c.getColumnType())) {
  //            for (Reference ref : c.getReferences()) {
  //              if (keys.get(ref.getName()) == null) {
  //                keys.put(ref.getName(), new HashSet());
  //              }
  //              if (row.get(ref.getName(), ref.getColumnType()) != null) {
  //                keys.get(ref.getName()).add(row.get(ref.getName(), ref.getColumnType()));
  //              }
  //            }
  //          } else if (REF_ARRAY.equals(c.getColumnType())) {
  //            for (Reference ref : c.getReferences()) {
  //              if (keys.get(ref.getName()) == null) {
  //                keys.put(ref.getName(), new HashSet());
  //              }
  //              if (row.get(ref.getName(), ref.getColumnType()) != null) {
  //                for (Object value : (Object[]) row.get(ref.getName(), ref.getColumnType())) {
  //                  keys.get(ref.getName()).add(value);
  //                }
  //              }
  //            }
  //          }
  //        }
  //        if (count % 100000 == 0) {
  //          logger.info("Validating row " + count);
  //        }
  //      }
  //
  //      logger.info(
  //          "indexed fkeys to check "
  //              + table.getName()
  //              + " in "
  //              + (System.currentTimeMillis() - start)
  //              + "ms");
  //
  //      // then check each key
  //      for (Column c : columns) {
  //        if (REF.equals(c.getColumnType()) || REF_ARRAY.equals(c.getColumnType())) {
  //          for (Reference ref : c.getReferences()) {
  //            List<Row> result =
  //                table
  //                    .getSchema()
  //                    .getTable(c.getRefTableName())
  //                    .query()
  //                    .where(f(ref.getRefTo(), Operator.EQUALS,
  // keys.get(ref.getName()).toArray()))
  //                    .retrieveRows();
  //            for (Row r : result) {
  //              keys.get(ref.getName())
  //                  .remove(
  //                      r.get(
  //                          ref.getRefTo(),
  //                          c.getRefTable().getColumn(ref.getRefTo()).getColumnType()));
  //            }
  //          }
  //        }
  //      }
  //      // if any of the lists non-empty we have missing
  //      String result = "";
  //      for (Column c : table.getMetadata().getColumns()) {
  //        if (REF.equals(c.getColumnType()) || REF_ARRAY.equals(c.getColumnType())) {
  //          for (Reference ref : c.getReferences()) {
  //            if (keys.get(ref.getName()).size() > 0) {
  //              result +=
  //                  "Keys missing for column "
  //                      + ref.getName()
  //                      + ": "
  //                      + keys.get(ref.getName()).stream().collect(Collectors.joining(","))
  //                      + ". ";
  //            }
  //          }
  //        }
  //      }
  //      logger.info(
  //          "validation for "
  //              + table.getName()
  //              + " took "
  //              + (System.currentTimeMillis() - start)
  //              + "ms");
  //      if (result.length() > 0) {
  //        throw new MolgenisException("Import of table " + table.getName() + " failed: " +
  // result);
  //      }
  //    }
  //  }

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
          logger.info("Imported {} into {}", count, table.getName());
        }
      }
      // remaining
      if (!batch.isEmpty()) {
        table.update(batch);
        logger.info("Imported {} into {}", count, table.getName());
      }
    }
  }
}
