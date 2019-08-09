package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;
import org.molgenis.MolgenisException;
import org.molgenis.ReferenceMultiple;
import org.molgenis.Table;
import org.molgenis.Type;
import org.molgenis.beans.ReferenceMultipleBean;

import java.util.Arrays;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.constraint;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

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
          new RefSqlColumn((SqlTable) this.getTable(), nameArray[i], toTable, toColumns[i]);
      getTable().addColumn(c);

      Field thisColumn = field(name(c.getName()), SqlTypeUtils.jooqTypeOf(c).nullable(false));
      table.getJooq().alterTable(table.getJooqTable()).addColumn(thisColumn).execute();
      saveColumnMetadata(c);
    }
    this.createCompositeForeignKey(toTable, toColumns);
    return getTable();
  }

  private void createCompositeForeignKey(String toTable, String... toColumn) {
    SqlTable table = (SqlTable) getTable();
    Name[] fields = Arrays.stream(getNameArray()).map(DSL::name).toArray(Name[]::new);
    Name[] toFields = Arrays.stream(toColumn).map(DSL::name).toArray(Name[]::new);
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
