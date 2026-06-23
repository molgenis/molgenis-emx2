package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;

import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Row;

public class SystemEditRolePrefixRowValueComputer implements RowValueComputer {

  @Override
  public void apply(List<Column> columns, Row row) {
    List<Column> toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .filter(c -> Constants.MG_EDIT_ROLE.equals(c.getName()))
            .toList();

    for (Column c : toValidateAndCompute) {
      row.setString(c.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
    }
  }
}
