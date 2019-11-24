package org.molgenis.emx2.sql;

import org.jooq.Field;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.jooq.impl.DSL.*;

/**
 * Todo:
 * <li>search
 * <li>where
 * <li>first
 * <li>after
 * <li>sort
 * <li>mref
 */
public class SqlJsonQuery {
  private SqlTableMetadata table;
  private List<Object> select;
  private List<Condition> where;
  private String[] search;

  public SqlJsonQuery(SqlTableMetadata table) {
    this.table = table;
  }

  public SqlJsonQuery(Table table) {
    this((SqlTableMetadata) table.getMetadata());
  }

  /**
   * you can create nested json selections following foreign relations, e.g.
   * ("name","status","category",List.of("name")) where category is a relationship to a table having
   * "name". Result will be like [{"name": ?, "status": ?, "category":{"name":?} }]
   */
  public SqlJsonQuery select(List select) {
    this.select = select;
    return this;
  }

  /** will search in all selected tables */
  public SqlJsonQuery search(String... terms) {
    this.search = terms;
    return this;
  }

  /** create filters based on paths, operators and values */
  public Condition where(String... path) {
    Condition c = new Condition(this, path);
    this.where.add(c);
    return c;
  }

  public String retrieve() {
    SelectJoinStep fromStep =
        table
            .getJooq()
            .select(getFields(table.getTableName(), select, table))
            .from(
                table(name(table.getSchema().getName(), table.getTableName()))
                    .as(table.getTableName()));

    System.out.println(fromStep.getSQL());

    return table
        .getJooq()
        .fetchOne("select json_strip_nulls(json_agg(item)) from (" + fromStep.getSQL() + ") item")
        .get(0, String.class);
  }

  private static List<Field> getFields(String parent, List<Object> select, SqlTableMetadata table) {
    List<Field> fields = new ArrayList<>();
    for (int i = 0; i < select.size(); i++) {
      Column column = getColumn(table, select.get(i));
      switch (column.getColumnType()) {
        case REF:
          fields.add(createRefSubselect(parent, column, getList(column, select.get(++i))));
          break;
        case REF_ARRAY:
          fields.add(createRefArraySubselect(parent, column, getList(column, select.get(++i))));
          break;
        default:
          fields.add(field(name(column.getColumnName()), SqlTypeUtils.jooqTypeOf(column)));
      }
    }
    return fields;
  }

  private static Field createRefArraySubselect(String parent, Column column, List select) {
    String fromAlias = parent + "/" + column.getColumnName();
    List<Field> fields = getFields(fromAlias, select, getRefTableMetadata(column));
    String sql =
        DSL.select(fields)
            .from(
                table(name(column.getTable().getSchema().getName(), column.getRefTableName()))
                    .as(fromAlias))
            .where(
                "{0} = ANY ({1})",
                field(name(column.getRefColumnName())), field(name(parent, column.getColumnName())))
            .getSQL();

    return field("(select json_agg(item) from (" + sql + ")item)").as(column.getColumnName());
  }

  private static Field createRefSubselect(String parent, Column column, List select) {
    String fromAlias = parent + "/" + column.getColumnName();
    List<Field> fields = getFields(fromAlias, select, getRefTableMetadata(column));
    String sql =
        DSL.select(fields)
            .from(
                table(name(column.getTable().getSchema().getName(), column.getRefTableName()))
                    .as(fromAlias))
            .where(
                field(name(column.getRefColumnName()))
                    .eq(field(name(parent, column.getColumnName()))))
            .getSQL();

    return field("(select row_to_json(item) from (" + sql + ")item)").as(column.getColumnName());
  }

  private static SqlTableMetadata getRefTableMetadata(Column column) {
    return (SqlTableMetadata)
        column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  private static List getList(Column column, Object o) {
    if (!(o instanceof List))
      throw new MolgenisException(
          "",
          "",
          "select error: expected list to follow REF column "
              + column.getColumnName()
              + " but found "
              + o);
    return (List) o;
  }

  private static Column getColumn(SqlTableMetadata table, Object o) {
    String colName = checkIsString(o);
    Column column = table.getColumn(colName);
    if (column == null)
      throw new MolgenisException(
          "",
          "",
          "Selection error: Column " + colName + " not found in table " + table.getTableName());
    return column;
  }

  private static String checkIsString(Object o) {
    if (!(o instanceof String))
      throw new MolgenisException(
          "query error",
          "query error",
          "Query only accept string or list type. E.g. 'name','tag',List.of('name')");
    return (String) o;
  }

  private void validate(Object... objects) {
    for (Object o : objects) {
      if (o instanceof List) validate(((List) o).toArray());
      else checkIsString(o);
    }
  }

  private static String random() {
    int leftLimit = 97; // letter 'a'
    int rightLimit = 122; // letter 'z'
    int targetStringLength = 10;
    Random random = new Random();
    StringBuilder buffer = new StringBuilder(targetStringLength);
    for (int i = 0; i < targetStringLength; i++) {
      int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
      buffer.append((char) randomLimitedInt);
    }
    return buffer.toString();
  }

  /** syntactic sugar */
  public Condition and(String... path) {
    return this.where(path);
  }

  public class Condition {
    private SqlJsonQuery query;
    private String[] path;
    private Operator operator;
    private Serializable[] values;

    public Condition(SqlJsonQuery query, String... path) {
      this.path = path;
      this.query = query;
    }

    public SqlJsonQuery eq(Serializable... values) {
      this.operator = Operator.EQUALS;
      this.values = values;
      return query;
    }
  }
}
