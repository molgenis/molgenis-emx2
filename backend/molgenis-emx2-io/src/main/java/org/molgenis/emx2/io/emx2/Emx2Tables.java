package org.molgenis.emx2.io.emx2;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.*;
import org.molgenis.emx2.*;
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

}
