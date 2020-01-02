package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.sql.SqlColumnUtils.getJoinTableName;
import static org.molgenis.emx2.sql.SqlColumnUtils.getMappedByColumn;
import static org.molgenis.emx2.sql.SqlQueryUtils.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.getJooqTable;

class SqlQueryRowHelper {
  private static Logger logger = LoggerFactory.getLogger(SqlQueryRowHelper.class);

  static List<Row> getRows(
      SqlTableMetadata table, SelectColumn select, Filter filter, String[] searchTerms) {

    // create select
    String tableAlias = table.getTableName();
    if (select == null || select.getColumNames().isEmpty()) {
      select = new SelectColumn(table.getTableName(), table.getColumnNames());
    }
    SelectSelectStep selectStep =
        table.getJooq().select(createSelectFields(table, tableAlias, select));

    // create from
    SelectJoinStep fromStep = selectStep.from(getJooqTable(table).as(tableAlias));

    // create joins
    SelectJoinStep joinStep = createJoins(fromStep, table, tableAlias, select, filter);

    // create filters,
    Condition conditions = createWheres(table, tableAlias, filter);

    // create search
    conditions =
        mergeConditions(conditions, createSearchCondition(table, tableAlias, select, searchTerms));

    if (conditions != null) {
      joinStep = (SelectJoinStep) joinStep.where(conditions);
    }
    if (select.getLimit() > 0) {
      joinStep = (SelectJoinStep) joinStep.limit(select.getLimit());
    }
    if (select.getOffset() > 0) {
      joinStep = (SelectJoinStep) joinStep.offset(select.getOffset());
    }
    return executeQuery(joinStep);
  }

  private static List<Field> createSelectFields(
      TableMetadata table, String tableAlias, SelectColumn select) {
    List<Field> fields = new ArrayList<>();

    validateSelect(select, table);

    for (Column column : table.getColumns()) {

      // if selected, we ignore aggregation here
      if (select != null && select.has(column.getName())) {
        // strip before first /
        String prefix = "";
        if (tableAlias.contains("/")) {
          prefix = tableAlias.substring(tableAlias.indexOf('/') + 1) + "/";
        }

        // inheritance
        String inheritAlias = getSubclassAlias(table, tableAlias, column);

        // check if nested
        if (!select.get(column.getName()).getColumNames().isEmpty()
            && (REF.equals(column.getColumnType())
                || REF_ARRAY.equals(column.getColumnType())
                || REFBACK.equals(column.getColumnType())
                || MREF.equals(column.getColumnType()))) {

          // check if not primary key that points to the parent table
          if (table.getInherit() == null || !column.getName().equals(table.getPrimaryKey())) {
            fields.addAll(
                createSelectFields(
                    column.getRefTable(),
                    tableAlias + "/" + column.getName(),
                    select != null ? select.get(column.getName()) : null));
          }
        } else {
          String columnAlias = prefix + column.getName();

          if (MREF.equals(column.getColumnType())) {
            fields.add(createMrefSubselect(column, inheritAlias).as(columnAlias));
          } else if (REFBACK.equals(column.getColumnType())) {
            fields.add(
                PostgresDSL.array(createBackrefSubselect(column, inheritAlias))
                    .as(column.getName()));
          } else {
            fields.add(
                field(name(inheritAlias, column.getName()), SqlTypeUtils.jooqTypeOf(column))
                    .as(columnAlias));
          }
        }
      }
    }
    return fields;
  }

  private static Condition createWheres(TableMetadata table, String tableAlias, Filter filter) {
    // validate
    validateFilter(table, filter);

    // create simple filters
    Condition condition = createFiltersForColumns(table, tableAlias, filter);

    // create filters for nested filters into join
    for (Column column : table.getColumns()) {
      if (filter != null && filter.has(column.getName())) {
        ColumnType type = column.getColumnType();

        if (REF.equals(type)
            || REF_ARRAY.equals(type)
            || MREF.equals(type)
            || REFBACK.equals(type)) {

          if (filter.has(column.getName())
              && !filter.getFilter(column.getName()).getSubfilters().isEmpty()) {
            // filters are on columns of the ref
            condition =
                mergeConditions(
                    condition,
                    createWheres(
                        column.getRefTable(),
                        tableAlias + "/" + column.getName(),
                        getFilterForRef(filter, column)));
          } else {

          }
        }
      }
    }
    return condition;
  }

  private static List<Row> executeQuery(Select query) {
    try {
      List<Row> result = new ArrayList<>();
      if (logger.isInfoEnabled()) {
        logger.info(query.getSQL(ParamType.INLINED));
      }
      Result<Record> fetch = query.fetch();
      for (Record r : fetch) {
        result.add(new SqlRow(r));
      }
      return result;
    } catch (DataAccessException dae) {
      throw new MolgenisException("Query failed", dae);
    } catch (SQLException sqle) {
      throw new MolgenisException("Query failed", sqle);
    }
  }

  private static Field<Object[]> createMrefSubselect(Column column, String tableAlias) {
    Column reverseToColumn = column.getTable().getPrimaryKeyColumn();
    // reverse column = primaryKey of 'getTable()' or in case of REFBACK it needs to found by
    // mappedBy
    for (Column c : column.getRefTable().getColumns()) {
      if (column.getName().equals(c.getMappedBy())) {
        reverseToColumn = c;
        break;
      }
    }
    return PostgresDSL.array(
        DSL.select(field(name(getJoinTableName(column), column.getName())))
            .from(name(column.getTable().getSchema().getName(), getJoinTableName(column)))
            .where(
                field(name(getJoinTableName(column), reverseToColumn.getName()))
                    .eq(field(name(tableAlias, reverseToColumn.getName())))));
  }

  static SelectConditionStep createBackrefSubselect(Column column, String tableAlias) {
    Column mappedBy = getMappedByColumn(column);
    switch (mappedBy.getColumnType()) {
      case REF:
        return DSL.select(field(name(column.getRefColumnName())))
            .from(
                name(mappedBy.getTable().getSchema().getName(), mappedBy.getTable().getTableName()))
            .where(
                field(name(mappedBy.getTable().getTableName(), mappedBy.getName()))
                    .eq(field(name(tableAlias, mappedBy.getRefColumnName()))));
      case REF_ARRAY:
        return DSL.select(field(name(column.getRefColumnName())))
            .from(
                name(mappedBy.getTable().getSchema().getName(), mappedBy.getTable().getTableName()))
            .where(
                "{0} = ANY ({1})",
                field(name(tableAlias, mappedBy.getRefColumnName())),
                field(name(mappedBy.getTable().getTableName(), mappedBy.getName())));
      default:
        throw new MolgenisException(
            "Internal error", "Refback for type not matched for column " + column.getName());
    }
  }
}
