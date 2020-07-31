// package org.molgenis.emx2.sql;
//
// import org.jooq.*;
// import org.jooq.conf.ParamType;
// import org.jooq.exception.DataAccessException;
// import org.jooq.util.postgres.PostgresDSL;
// import org.molgenis.emx2.Row;
// import org.molgenis.emx2.*;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
//
// import java.sql.SQLException;
// import java.util.ArrayList;
// import java.util.List;
//
// import static org.jooq.impl.DSL.*;
// import static org.molgenis.emx2.ColumnType.*;
// import static org.molgenis.emx2.sql.SqlQueryUtils.*;
// import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.getJooqTable;
//
// class SqlQueryRowsExecutor {
//  private static Logger logger = LoggerFactory.getLogger(SqlQueryRowsExecutor.class);
//
//  private SqlQueryRowsExecutor() {
//    // hide constructor
//  }
//
//  static List<Row> getRows(
//      SqlTableMetadata table, SelectColumn select, Filter filter, String[] searchTerms) {
//
//    String tableAlias = table.getTableName();
//    if (select == null || select.getColumNames().isEmpty()) {
//      select = new SelectColumn(table.getTableName(), table.getColumnNames());
//    }
//    SelectSelectStep selectStep =
//        table.getJooq().select(createSelectFields(table, tableAlias, select));
//
//    // create from
//    SelectJoinStep fromStep = selectStep.from(getJooqTable(table).as(tableAlias));
//
//    // create joins
//    fromStep = createJoins(fromStep, table, tableAlias, select, filter); // NOSONAR
//
//    // create filters,
//    Condition conditions = createWheres(table, tableAlias, filter);
//
//    // create search
//    conditions =
//        mergeConditions(conditions, createSearchCondition(table, tableAlias, select,
// searchTerms));
//
//    if (conditions != null) {
//      fromStep = (SelectJoinStep) fromStep.where(conditions);
//    }
//    if (select.getLimit() > 0) {
//      fromStep = (SelectJoinStep) fromStep.limit(select.getLimit());
//    }
//    if (select.getOffset() > 0) {
//      fromStep = (SelectJoinStep) fromStep.offset(select.getOffset());
//    }
//    return executeQuery(fromStep);
//  }
//
//  private static List<Field> createSelectFields(
//      TableMetadata table, String tableAlias, SelectColumn select) {
//    List<Field> fields = new ArrayList<>();
//
//    validateSelect(select, table);
//
//    for (Column column : table.getColumns()) {
//      // if selected, we ignore aggregation here
//      if (select != null && select.has(column.getName())) {
//        // strip before first /
//        String prefix = "";
//        if (tableAlias.contains("/")) {
//          prefix = tableAlias.substring(tableAlias.indexOf('/') + 1) + "/";
//        }
//
//        // inheritance
//        String inheritAlias = getSubclassAlias(table, tableAlias, column);
//
//        // check if nested
//        if (!select.getSubselect(column.getName()).getColumNames().isEmpty()
//            && column.isReference()) {
//
//          // check if not primary key that points to the parent table
//          if (table.getInherit() == null || table.getPrimaryKeys().contains(column.getName())) {
//            fields.addAll(
//                createSelectFields(
//                    column.getRefTable(),
//                    tableAlias + "/" + column.getName(),
//                    select != null ? select.getSubselect(column.getName()) : null));
//          }
//        } else {
//          String columnAlias = prefix + column.getName();
//
//          if (MREF.equals(column.getColumnType())) {
//            fields.add(SqlQueryGraphExecutor.createMrefSubselect(column,
// inheritAlias).as(columnAlias));
//          } else if (REFBACK.equals(column.getColumnType())) {
//            fields.add(
//                PostgresDSL.array(SqlQueryGraphExecutor.createBackrefSubselect(column,
// inheritAlias))
//                    .as(column.getName()));
//          } else if (REF.equals(column.getColumnType())
//              || REF_ARRAY.equals(column.getColumnType())) {
//            for (Reference ref : column.getRefColumns()) {
//              fields.add(
//                  field(name(inheritAlias, ref.getName()), ref.getJooqType()).as(columnAlias));
//            }
//          } else {
//            fields.add(
//                field(name(inheritAlias, column.getName()),
// column.getJooqType()).as(columnAlias));
//          }
//        }
//      }
//    }
//    return fields;
//  }
//
//  private static Condition createWheres(TableMetadata table, String tableAlias, Filter filter) {
//    // validate
//    validateFilter(table, filter);
//
//    // create simple filters
//    Condition condition = createFiltersForColumns(table, tableAlias, filter);
//
//    // create filters for nested filters into join
//    for (Column column : table.getColumns()) {
//      if (filter != null
//          && column.isReference()
//          && filter.getSubfilter(column.getName()) != null
//          && !filter.getSubfilter(column.getName()).getSubfilters().isEmpty()) {
//        // filters are on columns of the ref
//        condition =
//            mergeConditions(
//                condition,
//                createWheres(
//                    column.getRefTable(),
//                    tableAlias + "/" + column.getName(),
//                    getFilterForRef(filter, column)));
//      }
//    }
//    return condition;
//  }
//
//  private static List<Row> executeQuery(Select query) {
//    try {
//      List<Row> result = new ArrayList<>();
//      if (logger.isInfoEnabled()) {
//        logger.info(query.getSQL(ParamType.INLINED));
//      }
//      Result<Record> fetch = query.fetch();
//      for (Record r : fetch) {
//        result.add(new SqlRow(r));
//      }
//      return result;
//    } catch (DataAccessException dae) {
//      throw new MolgenisException("Query failed", dae);
//    } catch (SQLException sqle) {
//      throw new MolgenisException("Query failed", sqle);
//    }
//  }
//
// }
