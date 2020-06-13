package org.molgenis.emx2.sql;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.SelectJoinStep;
import org.jooq.util.postgres.PostgresDSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.NOT_EQUALS;
import static org.molgenis.emx2.sql.Constants.MG_TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.sql.SqlColumnUtils.getMappedByColumn;
import static org.molgenis.emx2.sql.SqlQueryRowHelper.createBackrefSubselect;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.getJooqTable;
import static org.molgenis.emx2.sql.SqlTypeUtils.getRefColumnType;
import static org.molgenis.emx2.utils.TypeUtils.*;
import static org.molgenis.emx2.utils.TypeUtils.toDateTimeArray;

class SqlQueryUtils {
  static final String QUERY_FAILED = "Query failed";
  private static final String OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE =
      "Operator %s is not support for column '%s'";
  private static final String BETWEEN_ERROR_MESSAGE =
      "Operator BETWEEEN a AND b expects even number of parameters to define each pair of a,b. Found: %s";
  static final String ANY_SQL = "{0} = ANY ({1})";

  private SqlQueryUtils() {
    // hide
  }

  static Condition mergeConditions(Condition conditions, Condition conditions2) {
    if (conditions == null) {
      if (conditions2 == null) {
        return null;
      } else {
        return conditions2;
      }
    } else {
      if (conditions2 == null) {
        return conditions;
      } else {
        return conditions.and(conditions2);
      }
    }
  }

  static SelectJoinStep createJoins(
      SelectJoinStep step,
      TableMetadata table,
      String leftAlias,
      SelectColumn select,
      Filter filter) {
    // create inheritance joins
    createInheritanceJoin(table, leftAlias, step);

    // create ref column joins
    for (Column column : table.getLocalColumns()) {
      if (isSelectedOrFiltered(column, select, filter)) {
        String rightAlias = leftAlias + "/" + column.getName();
        ColumnType type = column.getColumnType();
        String refCol = column.getRefColumnName();
        Condition condition = null;

        if ((select != null
                && select.has(column.getName())
                && !select.get(column.getName()).getColumNames().isEmpty())
            || (filter != null
                && filter.has(column.getName())
                && !filter.getFilter(column.getName()).getSubfilters().isEmpty())) {
          if (REF_ARRAY.equals(type)) {
            condition =
                condition(
                    ANY_SQL,
                    field(name(rightAlias, refCol)),
                    field(name(leftAlias, column.getName())));
          } else if (REF.equals(type)) {
            condition =
                field(name(leftAlias, column.getName())).eq(field(name(rightAlias, refCol)));
          } else if (REFBACK.equals(type)) {
            Column mappedBy = getMappedByColumn(column);
            refCol = mappedBy.getRefColumnName();
            if (REF.equals(mappedBy.getColumnType())) {
              condition =
                  field(name(leftAlias, refCol)).eq(field(name(rightAlias, mappedBy.getName())));
            } else if (REF_ARRAY.equals(mappedBy.getColumnType())) {
              condition =
                  condition(
                      ANY_SQL,
                      field(name(leftAlias, refCol)),
                      field(name(rightAlias, mappedBy.getName())));
            }
          }
        }
        if (condition != null) {
          step = step.leftJoin(getJooqTable(column.getRefTable()).as(rightAlias)).on(condition);
          createJoins(
              step,
              column.getRefTable(),
              rightAlias,
              select.get(column.getName()),
              getFilterForRef(filter, column));
        }
      }
    }
    return step;
  }

  static Condition createSearchCondition(
      SqlTableMetadata fromTable, String fromAlias, SelectColumn select, String[] searchTerms) {
    if (searchTerms != null && searchTerms.length > 0) {
      if (fromTable.getPrimaryKey() == null) {
        throw new MolgenisException(QUERY_FAILED, "Search failed because no primary key was set");
      }
      // BIG TODO THIS IS NOT CORRECT
      Field pkey = field(name(fromAlias, fromTable.getPrimaryKey()[0]));
      // create subquery
      SelectJoinStep sub =
          fromTable.getJooq().select(pkey).from(getJooqTable(fromTable).as(fromAlias));
      sub = createJoins(sub, fromTable, fromAlias, select, null);
      return pkey.in(sub.where(createFiltersForSearch(fromTable, fromAlias, select, searchTerms)));
    }
    return null;
  }

  private static Condition createFiltersForSearch(
      TableMetadata table, String tableAlias, SelectColumn select, String[] searchTerms) {

    // create local filters
    List<Condition> local = new ArrayList<>();
    for (String term : searchTerms) {
      for (String subTerm : term.split(" ")) {
        subTerm = subTerm.trim();
        // short terms with 'like', longer with trigram
        if (subTerm.length() <= 3)
          local.add(
              field(name(tableAlias, MG_TEXT_SEARCH_COLUMN_NAME))
                  .likeIgnoreCase("%" + subTerm + "%"));
        else {
          local.add(
              condition(
                  "word_similarity({0},{1}) > 0.6",
                  subTerm, field(name(tableAlias, MG_TEXT_SEARCH_COLUMN_NAME))));
        }
        //          condition(
        //              "to_tsvector({0}) @@ to_tsquery('" + term.trim() + ":*')",
        //              name(tableAlias, MG_TEXT_SEARCH_COLUMN_NAME)));
        //    }
      }
    }
    Condition searchCondition = and(local);

    // get from subpaths
    for (Column column : table.getLocalColumns()) {
      if (select != null && select.has(column.getName())) {
        String nextAlias = tableAlias + "/" + column.getName();
        ColumnType type = column.getColumnType();
        if (!select.get(column.getName()).getColumNames().isEmpty()
            && (REF_ARRAY.equals(type) || REF.equals(type) || REFBACK.equals(type))) {
          searchCondition =
              searchCondition.or(
                  createFiltersForSearch(
                      column.getRefTable(), nextAlias, select.get(column.getName()), searchTerms));
        }
      }
    }
    return searchCondition;
  }

  static String getSubclassAlias(TableMetadata table, String tableAlias, Column column) {
    String inheritAlias = tableAlias;
    if (!column.getTableName().equals(table.getTableName())) {
      inheritAlias = tableAlias + "+" + column.getTableName();
    }
    return inheritAlias;
  }

  static SelectJoinStep createInheritanceJoin(
      TableMetadata table, String tableAlias, SelectJoinStep from) {
    TableMetadata inherit = table.getInheritedTable();
    while (inherit != null) {
      String subTableAlias = tableAlias + "+" + inherit.getTableName();

      Condition[] conditions = new Condition[table.getPrimaryKey().length];
      for (int i = 0; i < conditions.length; i++) {
        String pkey = table.getPrimaryKey()[i];
        conditions[i] = field(name(tableAlias, pkey)).eq(field(name(subTableAlias, pkey)));
      }

      from = from.innerJoin(getJooqTable(inherit).as(subTableAlias)).on(conditions);
      inherit = inherit.getInheritedTable();
    }
    return from;
  }

  static Condition createFiltersForColumns(TableMetadata table, String tableAlias, Filter filter) {
    if (filter == null) return null;
    Condition condition = null;
    for (Column column : table.getColumns()) {
      Filter f = getFilterForRef(filter, column);
      // we only filter on fields, not if the relationships
      if (f != null && f.getSubfilters().isEmpty()) {
        // add the column filter(s)
        for (Map.Entry<org.molgenis.emx2.Operator, Object[]> entry : f.getConditions().entrySet()) {
          // check if inherited
          String subAlias = getSubclassAlias(table, tableAlias, column);
          // else
          Condition subCondition =
              createFilterCondition(subAlias, column, entry.getKey(), entry.getValue());
          if (condition != null) condition = condition.and(subCondition);
          else condition = subCondition;
        }
      }
    }
    return condition;
  }

  static boolean isSelectedOrFiltered(Column column, SelectColumn select, Filter filter) {
    return (select != null && select.has(column.getName()))
        || filter != null && filter.has(column.getName());
  }

  static Filter getFilterForRef(Filter filter, Column column) {
    if (filter != null) return filter.getFilter(column.getName());
    return null;
  }

  private static Condition createFilterCondition(
      String tableAlias, Column column, org.molgenis.emx2.Operator operator, Object[] values) {
    Name name = name(tableAlias, column.getName());
    ColumnType type = column.getColumnType();
    if (REF_ARRAY.equals(type) || REF.equals(type)) {
      type = getRefColumnType(column);
    }
    switch (type) {
      case TEXT:
      case STRING:
        return createTextFilter(name, operator, toStringArray(values));
      case BOOL:
        return createEqualsFilter(name, operator, toBoolArray(values));
      case UUID:
        return createEqualsFilter(name, operator, toUuidArray(values));
      case INT:
        return createOrdinalFilter(name, operator, toIntArray(values));
      case DECIMAL:
        return createOrdinalFilter(name, operator, toDecimalArray(values));
      case DATE:
        return createOrdinalFilter(name, operator, toDateArray(values));
      case DATETIME:
        return createOrdinalFilter(name, operator, toDateTimeArray(values));
        // todo improve filters for arrays
      case STRING_ARRAY:
      case TEXT_ARRAY:
        return createArrayEqualsFilter(name, operator, toStringArray(values));
      case BOOL_ARRAY:
        return createArrayEqualsFilter(name, operator, toBoolArray(values));
      case UUID_ARRAY:
        return createArrayEqualsFilter(name, operator, toUuidArray(values));
      case INT_ARRAY:
        return createArrayEqualsFilter(name, operator, toIntArray(values));
      case DECIMAL_ARRAY:
        return createArrayEqualsFilter(name, operator, toDecimalArray(values));
      case DATE_ARRAY:
        return createArrayEqualsFilter(name, operator, toDateArray(values));
      case DATETIME_ARRAY:
        return createArrayEqualsFilter(name, operator, toDateTimeArray(values));
      case REFBACK:
        if (!EQUALS.equals(operator)) {
          throw new MolgenisException(QUERY_FAILED, "Only EQUALS is implemented for REFBACK");
        }
        Column mappedBy = getMappedByColumn(column);
        if (REF.equals(mappedBy.getColumnType())) {
          // subselect on on the other table with link to this
          return condition(
              "{0} && {1}",
              SqlTypeUtils.getTypedValue(values, column),
              field(PostgresDSL.array(createBackrefSubselect(column, tableAlias))));
        } else {
          return condition("FAILED");
        }
        //      case MREF:
        //        return null;
      default:
        throw new SqlGraphQueryException(
            QUERY_FAILED,
            "Filter of '"
                + name
                + " failed: operator "
                + operator
                + " not supported for type "
                + column.getColumnType());
    }
  }

  private static Condition createEqualsFilter(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    if (EQUALS.equals(operator)) {
      return field(columnName).in(values);
    } else if (NOT_EQUALS.equals(operator)) {
      return not(field(columnName).in(values));
    } else {
      throw new SqlGraphQueryException(
          QUERY_FAILED, OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, columnName);
    }
  }

  private static Condition createArrayEqualsFilter(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    switch (operator) {
      case EQUALS:
        conditions.add(condition("{0} && {1}", values, field(columnName)));
        break;
      case NOT_EQUALS:
        not = true;
        conditions.add(condition("{0} && {1}", values, field(columnName)));
        break;
      default:
        throw new SqlGraphQueryException(
            QUERY_FAILED, OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, operator, columnName);
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition createTextFilter(
      Name columnName, org.molgenis.emx2.Operator operator, String[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (String value : values) {
      switch (operator) {
        case EQUALS:
          conditions.add(field(columnName).eq(value));
          break;
        case NOT_EQUALS:
          not = true;
          conditions.add(field(columnName).eq(value));
          break;
        case NOT_LIKE:
          not = true;
          conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          break;
        case LIKE:
          conditions.add(field(columnName).likeIgnoreCase("%" + value + "%"));
          break;
        case TRIGRAM_SEARCH:
          conditions.add(condition("word_similarity({0},{1}) > 0.6", value, columnName));
          break;
        case TEXT_SEARCH:
          conditions.add(
              condition(
                  "to_tsquery({0}) @@ to_tsvector({1})",
                  value.trim().replaceAll("\\s+", ":* & ") + ":*", columnName));
          break;
        default:
          throw new SqlGraphQueryException(
              QUERY_FAILED, OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, operator, columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  private static Condition createOrdinalFilter(
      Name columnName, org.molgenis.emx2.Operator operator, Object[] values) {
    List<Condition> conditions = new ArrayList<>();
    boolean not = false;
    for (int i = 0; i < values.length; i++) {
      switch (operator) {
        case EQUALS:
        case NOT_EQUALS:
          return createEqualsFilter(columnName, operator, values);
        case NOT_BETWEEN:
          not = true;
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(
                QUERY_FAILED, BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
          if (values[i] != null && values[i + 1] != null) {
            conditions.add(field(columnName).notBetween(values[i], values[i + 1]));
          } else if (values[i] != null && values[i + 1] == null) {
            conditions.add(field(columnName).lessOrEqual(values[i]));
          } else if (values[i] == null && values[i + 1] != null) {
            conditions.add(field(columnName).greaterOrEqual(values[i + 1]));
          } else {
            // nothing to do
          }
          i++; // NOSONAR
          break;
        case BETWEEN:
          if (i + 1 > values.length)
            throw new SqlGraphQueryException(
                QUERY_FAILED, BETWEEN_ERROR_MESSAGE, TypeUtils.toString(values));
          if (values[i] != null && values[i + 1] != null) {
            conditions.add(field(columnName).between(values[i], values[i + 1]));
          } else if (values[i] != null && values[i + 1] == null) {
            conditions.add(field(columnName).greaterOrEqual(values[i]));
          } else if (values[i] == null && values[i + 1] != null) {
            conditions.add(field(columnName).lessOrEqual(values[i + 1]));
          } else {
            // nothing to do
          }
          i++; // NOSONAR
          break;
        default:
          throw new SqlGraphQueryException(
              QUERY_FAILED, OPERATOR_NOT_SUPPORTED_ERROR_MESSAGE, operator, columnName);
      }
    }
    if (not) return not(or(conditions));
    else return or(conditions);
  }

  static void validateFilter(TableMetadata table, Filter filter) {
    if (filter != null) {
      for (Filter subFilter : filter.getSubfilters()) {
        if (table.getColumn(subFilter.getField()) == null) {
          throw new MolgenisException(
              QUERY_FAILED,
              "Filter column '"
                  + subFilter.getField()
                  + "' unknown in table '"
                  + table.getTableName()
                  + "'");
        }
      }
    }
  }

  static void validateSelect(SelectColumn select, TableMetadata table) {
    if (select != null) {
      for (String name : select.getColumNames()) {
        if (table.getColumn(name) == null) {
          throw new MolgenisException(
              QUERY_FAILED,
              "Select column '" + name + "' unknown in table '" + table.getTableName() + "'");
        }
      }
    }
  }
}
