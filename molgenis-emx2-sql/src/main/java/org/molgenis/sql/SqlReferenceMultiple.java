package org.molgenis.sql;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.impl.DSL;
import org.molgenis.MolgenisException;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;
import org.molgenis.metadata.ReferenceMultiple;

import java.util.Arrays;

import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.constraint;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

public class SqlReferenceMultiple extends ReferenceMultiple {
  public SqlReferenceMultiple(SqlTableMetadata table, Type type, String[] nameArray) {
    super(table, type, nameArray);
  }

  @Override
  public TableMetadata to(String toTable, String... toColumns) throws MolgenisException {
    SqlTableMetadata table = (SqlTableMetadata) getTable();

    String[] nameArray = getNameArray();
    if (nameArray == null || nameArray.length != toColumns.length)
      throw new MolgenisException(
          "invalid_foreign_key",
          "Foreign key reference invalid",
          "Create of foreign key from table '"
              + table.getTableName()
              + "' to table '"
              + toTable
              + "' failed: fromColumn and toColumn must have the same number of colums");

    for (int i = 0; i < nameArray.length; i++) {
      RefSqlColumnMetadata c =
          new RefSqlColumnMetadata(
              (SqlTableMetadata) this.getTable(), nameArray[i], toTable, toColumns[i]);
      getTable().addColumn(c);

      Field thisColumn = field(name(c.getColumnName()), SqlTypeUtils.jooqTypeOf(c).nullable(false));
      table.getJooq().alterTable(table.getJooqTable()).addColumn(thisColumn).execute();
      saveColumnMetadata(c);
    }
    this.createCompositeForeignKey(toTable, toColumns);
    return getTable();
  }

  private void createCompositeForeignKey(String toTable, String... toColumn) {
    SqlTableMetadata table = (SqlTableMetadata) getTable();
    Name[] fields = Arrays.stream(getNameArray()).map(DSL::name).toArray(Name[]::new);
    Name[] toFields = Arrays.stream(toColumn).map(DSL::name).toArray(Name[]::new);
    table
        .getJooq()
        .alterTable(table.getJooqTable())
        .add(
            constraint()
                .foreignKey(fields)
                .references(name(table.getSchema().getName(), toTable), toFields)
                .onUpdateCascade())
        .execute();
  }
}
