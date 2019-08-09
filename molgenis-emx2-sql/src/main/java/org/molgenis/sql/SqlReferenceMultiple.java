package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Name;
import org.molgenis.MolgenisException;
import org.molgenis.ReferenceMultiple;
import org.molgenis.Table;
import org.molgenis.Type;
import org.molgenis.beans.ReferenceMultipleBean;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.constraint;

public class SqlReferenceMultiple extends ReferenceMultipleBean implements ReferenceMultiple {
  public SqlReferenceMultiple(SqlTable table, Type type, String[] nameArray) {
    super(table, type, nameArray);
  }

  @Override
  public Table to(String toTable, String... toColumns) throws MolgenisException {
    SqlTable table = (SqlTable) getTable();

    String[] nameArray = getNameArray();
    if (nameArray == null || nameArray.length != toColumns.length)
      throw new MolgenisException("Reference toColumn must be of same size");

    for (int i = 0; i < nameArray.length; i++) {
      RefSqlColumn c =
          new RefSqlColumn((SqlTable) this.getTable(), nameArray[i], toTable, toColumns[i], false);
      getTable().addColumn(c);

      Field thisColumn = field(name(c.getName()), SqlTypeUtils.jooqTypeOf(c).nullable(false));
      table.getJooq().alterTable(table.getJooqTable()).addColumn(thisColumn).execute();
    }
    this.createCompositeForeignKey(toTable, toColumns);
    return getTable();
  }

  private void createCompositeForeignKey(String toTable, String... toColumn) {
    SqlTable table = (SqlTable) getTable();
    Name[] fields = Arrays.stream(getNameArray()).map(s -> name(s)).toArray(Name[]::new);
    Name[] toFields = Arrays.stream(toColumn).map(s -> name(s)).toArray(Name[]::new);
    table
        .getJooq()
        .alterTable(table.getJooqTable())
        .add(
            constraint()
                .foreignKey(fields)
                .references(name(table.getSchemaName(), toTable), toFields)
                .onUpdateCascade())
        .execute();
  }
}
