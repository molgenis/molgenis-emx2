package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.beans.QueryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;

public class SqlQuery extends QueryBean implements Query {
  private SqlTableMetadata from;
  private static Logger logger = LoggerFactory.getLogger(SqlQuery.class);

  // tables that have selected fields so need to be included in the join
  private Map<String, SqlColumn> tableAliases = new TreeMap<>();

  public SqlQuery(SqlTableMetadata from) {
    this.from = from;
  }

  @Override
  public List<Row> retrieve() {
    try {
      tableAliases.put(from.getTableName(), null);
      SelectSelectStep selectStep = createSelectClause();
      SelectJoinStep fromStep = createFromClause(selectStep);
      SelectJoinStep whereStep = createWhereClause(fromStep);
      return executeQuery(whereStep);
    } catch (MolgenisException e) {
      throw e;
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(dae);
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed", e2.getCause().getMessage(), e2);
      else throw new MolgenisException("Query failed", "Unknown error", e2);
    }
  }

  private SelectSelectStep createSelectClause() {

    Collection<String> selectList = getSelectList();

    // in case of empty select, we select the columns of the 'from' table
    if (selectList.isEmpty()) {
      selectList = from.getColumnNames();
    }

    // for each column create a Field
    List<Field> fields = new ArrayList<>();
    for (String columnAlias : selectList) {
      String[] path = getPath(columnAlias);

      StringBuilder tableAliasBuilder = new StringBuilder(from.getTableName());
      SqlColumn column = getColumn(from, path, tableAliasBuilder, tableAliases);
      String tableAlias = tableAliasBuilder.toString();

      // add as fields to list
      fields.add(getFieldForColumn(column, tableAlias, columnAlias));
    }

    // return jooq select step
    SelectSelectStep selectStep;
    if (!fields.isEmpty()) selectStep = getJooq().select(fields); // should never happen
    else selectStep = getJooq().select();
    return selectStep;
  }

  private SelectJoinStep createFromClause(SelectSelectStep selectStep) {

    SelectJoinStep fromStep =
        selectStep.from(
            table(name(from.getSchema().getName(), from.getTableName()))
                .as(name(from.getTableName())));

    // join the tables for all paths beyond primary 'from' table
    for (Map.Entry<String, SqlColumn> tableAlias : tableAliases.entrySet()) {
      String[] path = getPath(tableAlias.getKey());
      if (path.length > 1) { // ignore the 'from'
        SqlColumn fkey = tableAlias.getValue();
        String leftAlias = String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
        switch (fkey.getColumnType()) {
          case REF:
            fromStep = createRefJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
            break;
          case REF_ARRAY:
            fromStep = createRefArrayJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
            break;
          case REFBACK:
            fromStep = createRefbackJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
          case MREF:
            fromStep = createMrefJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
            break;
          default:
            // leaves of the paths we don't need
            break;
        }
      }
    }
    return fromStep;
  }

  private SelectJoinStep createWhereClause(SelectJoinStep fromStep) {
    Condition filterCondition = createFilterConditions(from, getWhereLists(), tableAliases);
    Condition searchCondition = createSearchConditions(getSearchList(), tableAliases);
    if (filterCondition != null) {
      if (searchCondition != null) {
        fromStep.where(filterCondition).and(searchCondition);
      } else {
        fromStep.where(filterCondition);
      }
    } else if (searchCondition != null) {
      fromStep.where(searchCondition);
    }
    return fromStep;
  }

  private static List<Row> executeQuery(SelectJoinStep whereStep) throws SQLException {
    List<Row> result = new ArrayList<>();
    if (logger.isInfoEnabled()) {
      logger.info(whereStep.getSQL(ParamType.INLINED));
    }
    Result<Record> fetch = whereStep.fetch();
    for (Record r : fetch) {
      result.add(new SqlRow(r));
    }
    return result;
  }

  // helper methods below

  private static SelectJoinStep createMrefJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    String joinTable = fkey.getMappedBy();

    // to link table
    fromStep =
        fromStep
            .leftJoin(
                table(name(fkey.getTable().getSchema().getName(), joinTable)).as(name(joinTable)))
            .on(
                field(name(joinTable, fkey.getRefColumnName()))
                    .eq(field(name(leftAlias, fkey.getName()))));
    // to other end of the mref
    fromStep = createRefJoin(fromStep, tableAlias, fkey, joinTable);
    return fromStep;
  }

  private static SelectJoinStep createRefArrayJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    fromStep =
        fromStep
            .leftJoin(
                table(name(fkey.getTable().getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                "{0} = ANY ({1})",
                field(name(tableAlias, fkey.getRefColumnName())),
                field(name(leftAlias, fkey.getName())));
    return fromStep;
  }

  private static SelectJoinStep createRefJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    fromStep =
        fromStep
            .leftJoin(
                table(name(fkey.getTable().getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                field(name(leftAlias, fkey.getName()))
                    .eq(field(name(tableAlias, fkey.getRefColumnName()))));
    return fromStep;
  }

  private SelectJoinStep createRefbackJoin(
      SelectJoinStep fromStep, String tableAlias, SqlColumn fkey, String leftAlias) {
    Column mappedBy = fkey.getMappedByColumn();
    switch (mappedBy.getColumnType()) {
      case REF:
        return fromStep
            .leftJoin(
                table(name(fkey.getTable().getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                field(name(leftAlias, mappedBy.getName()))
                    .eq(field(name(tableAlias, mappedBy.getRefColumnName()))));
    }
    throw new MolgenisException(
        "Internal error",
        "Unsupported refback type for column '" + fkey.getName() + "' createRefBackJoin");
  }

  private static Field<Object[]> createMrefSubselect(SqlColumn column, String tableAlias) {
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
        DSL.select(field(name(column.getJoinTableName(), column.getName())))
            .from(name(column.getTable().getSchema().getName(), column.getJoinTableName()))
            .where(
                field(name(column.getJoinTableName(), reverseToColumn.getName()))
                    .eq(field(name(tableAlias, reverseToColumn.getName())))));
  }

  private Condition createFilterConditions(
      TableMetadata from, List<Where> whereList, Map<String, SqlColumn> tableAliases) {
    Condition conditions = null;
    for (Where w : whereList) {
      Condition newCondition;
      newCondition = createFilterCondition(w, from, tableAliases);
      if (newCondition != null) {
        if (conditions == null) conditions = newCondition;
        else {
          conditions = conditions.and(newCondition);
        }
      }
    }

    return conditions;
  }

  private Condition createFilterCondition(
      Where w, TableMetadata from, Map<String, SqlColumn> tableAliases) {

    // in case of field operator
    String[] path = getPath(w.getPath());
    StringBuilder tableAliasBuilder = new StringBuilder(from.getTableName());
    if (path.length > 1) {
      tableAliasBuilder.append(
          "/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1)));
    }
    String tableAlias = tableAliasBuilder.toString();
    Name selector = name(tableAlias, path[path.length - 1]);
    switch (w.getOperator()) {
      case EQUALS:
        SqlColumn column = getColumn(from, path, tableAliasBuilder, tableAliases);
        ColumnType type = column.getColumnType();
        if (REF_ARRAY.equals(type)
            || STRING_ARRAY.equals(type)
            || INT_ARRAY.equals(type)
            || DECIMAL_ARRAY.equals(type)
            || BOOL_ARRAY.equals(type)
            || DATE_ARRAY.equals(type)
            || DATETIME_ARRAY.equals(type)
            || TEXT_ARRAY.equals(type)
            || UUID_ARRAY.equals(type)) {
          return condition(
              "{0} && {1}", SqlTypeUtils.getTypedValue(w.getValues(), column), field(selector));
        } else if (REFBACK.equals(type)) {
          SqlColumn mappedBy = column.getMappedByColumn();
          String tableName = mappedBy.getTable().getTableName();
          String schemaName = mappedBy.getTable().getSchema().getName();
          switch (mappedBy.getColumnType()) {
            case REF:
              // subselect on on the other table with link to this
              return condition(
                  "{0} && {1}",
                  SqlTypeUtils.getTypedValue(w.getValues(), column),
                  PostgresDSL.array(createBackrefSubselect(column, tableAlias)));
          }
        } else if (MREF.equals(type)) {
          return condition(
              "{0} && {1}",
              SqlTypeUtils.getTypedValue(w.getValues(), column),
              createMrefSubselect(column, tableAlias));
        } else {
          Object[] values = w.getValues();
          for (int i = 0; i < values.length; i++) {
            values[i] = SqlTypeUtils.getTypedValue(values[i], column);
          }
          return field(selector).in(values);
        }
      default:
        throw new MolgenisException(
            "Query failed", "Where clause '" + w.toString() + "' is not supported");
    }
  }

  private static Condition createSearchConditions(
      List<String> searchList, Map<String, SqlColumn> tableAliases) {
    if (searchList.isEmpty()) return null;
    String search = String.join("|", searchList);
    Condition searchCondition = null;
    for (String tableAlias : tableAliases.keySet()) {
      Condition condition =
          condition(
              name(tableAlias, MG_SEARCH_INDEX_COLUMN_NAME) + " @@ to_tsquery('" + search + "')");
      if (searchCondition == null) {
        searchCondition = condition;
      } else {
        searchCondition.or(condition);
      }
    }
    return searchCondition;
  }

  private static String[] getPath(String s) {
    // todo check for escaping with //
    return s.split("/");
  }

  private static SqlColumn getColumn(
      TableMetadata t,
      String[] path,
      StringBuilder tableAliasBuilder,
      Map<String, SqlColumn> tableAliases) {

    // table Alias builder might be null when getColumn us used in the 'from' clause
    SqlColumn c = (SqlColumn) t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "Query failed", "Column '" + path[0] + "' cannot be found in table " + t.getTableName());

    if (path.length == 1) {

      // in case of inherited field we might need a 'parent' table
      String tableName = c.getTable().getTableName();
      while (!tableName.equals(t.getTableName())) {
        tableAliasBuilder.append("/" + t.getPrimaryKey());
        tableAliases.put(tableAliasBuilder.toString(), (SqlColumn) t.getPrimaryKeyColumn());
        t = t.getInheritedTable();
      }

      return c;
    } else {
      tableAliasBuilder.append("/" + path[0]);
      tableAliases.put(tableAliasBuilder.toString(), c);

      // navigate the fkey
      return getColumn(
          t.getSchema().getTableMetadata(c.getRefTableName()),
          Arrays.copyOfRange(path, 1, path.length),
          tableAliasBuilder,
          tableAliases);
    }
  }

  private Field getFieldForColumn(SqlColumn column, String tableAlias, String columnAlias) {
    if (MREF.equals(column.getColumnType())) {
      return createMrefSubselect(column, tableAlias).as(columnAlias);
    } else if (REFBACK.equals(column.getColumnType())) {
      return PostgresDSL.array(createBackrefSubselect(column, tableAlias)).as(column.getName());
    } else {
      return field(name(tableAlias, column.getName()), SqlTypeUtils.jooqTypeOf(column))
          .as(columnAlias);
    }
  }

  private SelectConditionStep createBackrefSubselect(SqlColumn column, String tableAlias) {
    SqlColumn mappedBy = column.getMappedByColumn();
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

  private DSLContext getJooq() {
    return from.getJooq();
  }
}
