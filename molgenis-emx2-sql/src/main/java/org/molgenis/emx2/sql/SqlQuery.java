package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.beans.QueryBean;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Where;
import org.molgenis.emx2.utils.MolgenisException;

import java.sql.SQLException;
import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.sql.Constants.MG_SEARCH_INDEX_COLUMN_NAME;

public class SqlQuery extends QueryBean implements Query {
  private SqlTableMetadata from;

  // tables that have selected fields so need to be included in the join
  private Map<String, Column> tableAliases = new TreeMap<>();

  public SqlQuery(SqlTableMetadata from) {
    this.from = from;
  }

  @Override
  public List<Row> retrieve() {
    try {
      tableAliases.put(from.getTableName(), null);
      SelectSelectStep selectStep = createSelect();
      SelectJoinStep fromStep = createFrom(selectStep);
      SelectJoinStep whereStep = createWhere(fromStep);
      return executeQuery(whereStep);
    } catch (MolgenisException e) {
      throw e;
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(dae);
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed:" + e2.getCause().getMessage(), e2);
      else throw new MolgenisException(e2);
    }
  }

  private SelectSelectStep createSelect() {

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

  private Field getFieldForColumn(Column column, String tableAlias, String columnAlias) {
    if (MREF.equals(column.getColumnType())) {
      return createMrefSubselect(column, tableAlias).as(columnAlias);
    } else {
      return field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column))
          .as(columnAlias);
    }
  }

  private SelectJoinStep createFrom(SelectSelectStep selectStep) {

    SelectJoinStep fromStep =
        selectStep.from(
            table(name(from.getSchema().getName(), from.getTableName()))
                .as(name(from.getTableName())));

    // join the tables for all paths beyond primary 'from' table
    for (String tableAlias : tableAliases.keySet()) {
      String[] path = getPath(tableAlias);
      if (path.length > 1) { // ignore the 'from'
        Column fkey = tableAliases.get(tableAlias);
        // Column fkey = getColumn(from, Arrays.copyOfRange(path, 1, path.length), null);
        String leftAlias = String.join("/", Arrays.copyOfRange(path, 0, path.length - 1));
        switch (fkey.getColumnType()) {
          case REF:
            fromStep = createRefJoin(fromStep, tableAlias, fkey, leftAlias);
            break;
          case REF_ARRAY:
            fromStep = createRefArrayJoin(fromStep, tableAlias, fkey, leftAlias);
            break;
          case MREF:
            fromStep = createMrefJoin(fromStep, tableAlias, fkey, leftAlias);
            break;
          default:
            // leaves of the paths we don't need
            break;
        }
      }
    }
    return fromStep;
  }

  private SelectJoinStep createWhere(SelectJoinStep fromStep) {
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
    System.out.println(whereStep.getSQL());
    Result<Record> fetch = whereStep.fetch();
    for (Record r : fetch) {
      result.add(new SqlRow(r));
    }
    return result;
  }

  private DSLContext getJooq() {
    return from.getJooq();
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
                    .eq(field(name(leftAlias, fkey.getColumnName()))));
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
                field(name(leftAlias, fkey.getColumnName())));
    return fromStep;
  }

  private static SelectJoinStep createParentJoin() {
    return null;
  }

  private static SelectJoinStep createRefJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    fromStep =
        fromStep
            .leftJoin(
                table(name(fkey.getTable().getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                field(name(leftAlias, fkey.getColumnName()))
                    .eq(field(name(tableAlias, fkey.getRefColumnName()))));
    return fromStep;
  }

  /** subselect for mref in select and/or filter clauses */
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
            "invalid_query",
            "Creation of filter condition failed",
            "Where clause '" + w.toString() + "' is not supported");
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

  /** utility methods below */
  private static String[] getPath(String s) {
    // todo check for escaping with //
    return s.split("/");
  }

  /** recursive getColumn */
  private static Column getColumn(
      TableMetadata t,
      String[] path,
      StringBuilder tableAliasBuilder,
      Map<String, Column> tableAliases) {

    // table Alias builder might be null when getColumn us used in the 'from' clause
    Column c = t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "undefined_column",
          "Column not found",
          "Column '" + path[0] + "' cannot be found in table " + t.getTableName());

    if (path.length == 1) {

      // in case of inherited field we might need a 'parent' table
      String tableName = c.getTable().getTableName();
      while (!tableName.equals(t.getTableName())) {
        tableAliasBuilder.append("/" + t.getPrimaryKey()[0]);
        tableAliases.put(tableAliasBuilder.toString(), t.getColumn(t.getPrimaryKey()[0]));
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
}
