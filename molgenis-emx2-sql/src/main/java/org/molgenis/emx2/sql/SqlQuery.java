package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.beans.QueryBean;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Where;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;

public class SqlQuery extends QueryBean implements Query {
  private SqlTableMetadata from;
  private static Logger logger = LoggerFactory.getLogger(SqlQuery.class);

  // tables that have selected fields so need to be included in the join
  private Map<String, Column> tableAliases = new TreeMap<>();

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
      Column column = getColumn(from, path, tableAliasBuilder, tableAliases);
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
    for (Map.Entry<String, Column> tableAlias : tableAliases.entrySet()) {
      String[] path = getPath(tableAlias.getKey());
      if (path.length > 1) { // ignore the 'from'
        Column fkey = tableAlias.getValue();
        String leftAlias = String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
        switch (fkey.getColumnType()) {
          case REF:
            fromStep = createRefJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
            break;
          case REF_ARRAY:
            fromStep = createRefArrayJoin(fromStep, tableAlias.getKey(), fkey, leftAlias);
            break;
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
    String joinTable = fkey.getMrefJoinTableName();

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

  private static Field<Object[]> createMrefSubselect(Column column, String tableAlias) {
    return PostgresDSL.array(
        DSL.select(field(name(column.getMrefJoinTableName(), column.getRefColumnName())))
            .from(name(column.getTable().getSchema().getName(), column.getMrefJoinTableName()))
            .where(
                field(name(column.getMrefJoinTableName(), column.getReverseRefColumn()))
                    .eq(field(name(tableAlias, column.getReverseRefColumn())))));
  }

  private static Condition createFilterConditions(
      TableMetadata from, List<Where> whereList, Map<String, Column> tableAliases) {
    Condition conditions = null;
    for (Where w : whereList) {
      Condition newCondition = null;
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

  private static Condition createFilterCondition(
      Where w, TableMetadata from, Map<String, Column> tableAliases) {

    // in case of field operator
    String[] path = getPath(w.getPath());
    StringBuilder tableAliasBuilder = new StringBuilder(from.getTableName());
    if (path.length > 1) {
      tableAliasBuilder.append(
          "/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1)));
    }
    Name selector = name(tableAliasBuilder.toString(), path[path.length - 1]);
    switch (w.getOperator()) {
      case EQUALS:
        // type check
        Object[] values = w.getValues();
        for (int i = 0; i < values.length; i++)
          values[i] =
              SqlTypeUtils.getTypedValue(
                  values[i], getColumn(from, path, tableAliasBuilder, tableAliases));
        return field(selector).in(values);
      case ANY:
        Column column = getColumn(from, path, tableAliasBuilder, tableAliases);
        if (MREF.equals(column.getColumnType())) {
          return condition(
              "{0} && {1}",
              SqlTypeUtils.getTypedValue(w.getValues(), column),
              createMrefSubselect(column, tableAliasBuilder.toString()));
        } else {
          return condition(
              "{0} && {1}", SqlTypeUtils.getTypedValue(w.getValues(), column), field(selector));
        }
      default:
        throw new MolgenisException(
            "Query failed", "Where clause '" + w.toString() + "' is not supported");
    }
  }

  private static Condition createSearchConditions(
      List<String> searchList, Map<String, Column> tableAliases) {
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

  private static Column getColumn(
      TableMetadata t,
      String[] path,
      StringBuilder tableAliasBuilder,
      Map<String, Column> tableAliases) {

    // table Alias builder might be null when getColumn us used in the 'from' clause
    Column c = t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "Query failed", "Column '" + path[0] + "' cannot be found in table " + t.getTableName());

    if (path.length == 1) {

      // in case of inherited field we might need a 'parent' table
      String tableName = c.getTable().getTableName();
      while (!tableName.equals(t.getTableName())) {
        tableAliasBuilder.append("/" + t.getPrimaryKey());
        tableAliases.put(tableAliasBuilder.toString(), t.getColumn(t.getPrimaryKey()));
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

  private Field getFieldForColumn(Column column, String tableAlias, String columnAlias) {
    if (MREF.equals(column.getColumnType())) {
      return createMrefSubselect(column, tableAlias).as(columnAlias);
    } else {
      return field(name(tableAlias, column.getName()), SqlTypeUtils.jooqTypeOf(column))
          .as(columnAlias);
    }
  }

  private DSLContext getJooq() {
    return from.getJooq();
  }
}
