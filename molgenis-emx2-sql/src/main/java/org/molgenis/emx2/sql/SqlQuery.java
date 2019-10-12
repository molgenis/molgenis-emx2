package org.molgenis.emx2.sql;

import org.jooq.*;
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

  private TableMetadata from;
  private DSLContext jooq;

  public SqlQuery(TableMetadata from, DSLContext jooq) {
    this.from = from;
    this.jooq = jooq;
  }

  @Override
  public List<Row> retrieve() {

    try {
      Set<String> tableAliases = new TreeSet<>();
      SelectSelectStep selectStep = createSelectSelectStep(tableAliases);
      SelectJoinStep fromStep = createFromStep(tableAliases, selectStep);
      SelectJoinStep whereStep = createWhereStep(tableAliases, fromStep);

      System.out.println(fromStep.getSQL());

      return queryRows(whereStep);
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e2) {
      if (e2.getCause() != null)
        throw new MolgenisException("Query failed:" + e2.getCause().getMessage(), e2);
      else throw new MolgenisException(e2);
    }
  }

  private List<Row> queryRows(SelectJoinStep whereStep) throws SQLException {
    List<Row> result = new ArrayList<>();
    Result<Record> fetch = whereStep.fetch();
    for (Record r : fetch) {
      result.add(new SqlRow(r));
    }
    return result;
  }

  private SelectJoinStep createWhereStep(Set<String> tableAliases, SelectJoinStep fromStep) {
    Condition filterCondition = createFilterConditions(from.getTableName());
    Condition searchCondition = createSearchConditions(tableAliases);
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

  private SelectJoinStep createFromStep(Set<String> tableAliases, SelectSelectStep selectStep) {
    SelectJoinStep fromStep =
        selectStep.from(table(name(from.getSchema().getName(), from.getTableName())));
    for (String tableAlias : tableAliases) {
      String[] path = getPath(tableAlias);
      if (path.length > 1) {
        Column fkey = getColumn(from, Arrays.copyOfRange(path, 1, path.length));
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
            break;
        }
      }
    }
    return fromStep;
  }

  private SelectJoinStep createMrefJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    String joinTable = fkey.getMrefJoinTableName();

    // to link table
    fromStep =
        fromStep
            .leftJoin(table(name(from.getSchema().getName(), joinTable)).as(name(joinTable)))
            .on(
                field(name(joinTable, fkey.getRefColumnName()))
                    .eq(field(name(leftAlias, fkey.getColumnName()))));
    // to other end of the mref
    fromStep = createRefJoin(fromStep, tableAlias, fkey, joinTable);
    return fromStep;
  }

  private SelectJoinStep createRefArrayJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    fromStep =
        fromStep
            .leftJoin(
                table(name(from.getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                "{0} = ANY ({1})",
                field(name(tableAlias, fkey.getRefColumnName())),
                field(name(leftAlias, fkey.getColumnName())));
    return fromStep;
  }

  private SelectJoinStep createRefJoin(
      SelectJoinStep fromStep, String tableAlias, Column fkey, String leftAlias) {
    fromStep =
        fromStep
            .leftJoin(
                table(name(from.getSchema().getName(), fkey.getRefTableName()))
                    .as(name(tableAlias)))
            .on(
                field(name(leftAlias, fkey.getColumnName()))
                    .eq(field(name(tableAlias, fkey.getRefColumnName()))));
    return fromStep;
  }

  private SelectSelectStep createSelectSelectStep(Set<String> tableAliases) {
    List<String> selectList = getSelectList();
    if (selectList.isEmpty()) {
      for (Column c : from.getColumns()) {
        selectList.add(c.getColumnName());
      }
    }
    List<Field> fields = new ArrayList<>();
    for (String select : selectList) {
      String[] path = getPath(select);
      Column column = getColumn(from, path);
      // table alias = from.getTableName + path
      String tableAlias = from.getTableName();
      if (path.length > 1) {
        for (int i = 0; i < path.length - 1; i++) {
          tableAlias += "/" + path[i];
        }
      }
      if (MREF.equals(column.getColumnType())) {
        // select array(mref_col from mreftable...)
        fields.add(createMrefSubselect(column, tableAlias).as(select));
      } else {
        //
        fields.add(
            field(name(tableAlias, column.getColumnName()), SqlTypeUtils.jooqTypeOf(column))
                .as(select));
      }
      tableAliases.add(tableAlias);
    }
    SelectSelectStep selectStep;
    if (!fields.isEmpty()) selectStep = jooq.select(fields);
    else selectStep = jooq.select();
    return selectStep;
  }

  private Condition createSearchConditions(Set<String> tableAliases) {
    if (getSearchList().size() == 0) return null;
    String search = String.join("|", getSearchList());
    Condition searchCondition = null;
    for (String tableAlias : tableAliases) {
      Condition condition =
          condition(
              name(tableAlias, MG_SEARCH_INDEX_COLUMN_NAME) + " @@ to_tsquery('" + search + "' )");
      if (searchCondition == null) {
        searchCondition = condition;
      } else {
        searchCondition.or(condition);
      }
    }
    return searchCondition;
  }

  /** subselect for mref in select and/or filter clauses */
  private Field<Object[]> createMrefSubselect(Column column, String tableAlias) {
    return PostgresDSL.array(
        DSL.select(field(name(column.getMrefJoinTableName(), column.getRefColumnName())))
            .from(name(from.getSchema().getName(), column.getMrefJoinTableName()))
            .where(
                field(name(column.getMrefJoinTableName(), column.getReverseRefColumn()))
                    .eq(field(name(tableAlias, column.getReverseRefColumn())))));
  }

  private Condition createFilterConditions(String tableName) {
    Condition conditions = null;
    for (Where w : this.getWhereLists()) {
      Condition newCondition = null;
      newCondition = createFilterCondition(w, tableName);
      if (newCondition != null) {
        if (conditions == null) conditions = newCondition;
        else {
          conditions = conditions.and(newCondition);
        }
      }
    }

    return conditions;
  }

  private Condition createFilterCondition(Where w, String tableName) {
    // in case of field operator
    String[] path = getPath(w.getPath());
    StringBuilder tableAlias = new StringBuilder(tableName);

    if (path.length > 1) {
      tableAlias.append("/" + String.join("/", Arrays.copyOfRange(path, 0, path.length - 1)));
    }
    Name selector = name(tableAlias.toString(), path[path.length - 1]);
    switch (w.getOperator()) {
      case EQUALS:
        // type check
        Object[] values = w.getValues();
        for (int i = 0; i < values.length; i++)
          values[i] = SqlTypeUtils.getTypedValue(values[i], getColumn(from, path));
        return field(selector).in(values);
      case ANY:
        Column column = getColumn(from, path);
        if (MREF.equals(column.getColumnType())) {
          return condition(
              "{0} && {1}",
              SqlTypeUtils.getTypedValue(w.getValues(), column),
              createMrefSubselect(column, tableAlias.toString()));
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

  private String[] getPath(String s) {
    // todo check for escaping with //
    return s.split("/");
  }

  /** recursive getColumn */
  private Column getColumn(TableMetadata t, String[] path) {
    Column c = t.getColumn(path[0]);
    if (c == null)
      throw new MolgenisException(
          "undefined_column",
          "Column not found",
          "Column '" + path[0] + "' cannot be found in table " + t.getTableName());
    if (path.length == 1) {
      return c;
    } else {
      return getColumn(
          t.getSchema().getTableMetadata(c.getRefTableName()),
          Arrays.copyOfRange(path, 1, path.length));
    }
  }
}
