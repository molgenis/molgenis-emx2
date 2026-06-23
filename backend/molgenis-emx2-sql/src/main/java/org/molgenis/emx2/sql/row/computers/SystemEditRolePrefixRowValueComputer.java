package org.molgenis.emx2.sql.row.computers;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;

public class SystemEditRolePrefixRowValueComputer implements RowValueComputer {

  @Override
  public void apply(Column column, Row row) {
    row.setString(
        column.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
  }

  @Override
  public boolean shouldComputeForColumn(Column column, Row row) {
    return Constants.MG_EDIT_ROLE.equals(column.getName());
  }
}
