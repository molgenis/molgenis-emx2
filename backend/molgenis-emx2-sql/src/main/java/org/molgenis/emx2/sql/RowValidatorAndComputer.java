package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.AUTO_ID;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.row.computers.*;

public class RowValidatorAndComputer {

  public static final SystemEditRolePrefixRowValueComputer ROLE_COMPUTER =
      new SystemEditRolePrefixRowValueComputer();

  private final List<RowValueComputer> computers;
  private final List<Column> toValidateAndCompute;

  public RowValidatorAndComputer(List<Column> columns) {
    this.computers =
        List.of(
            ROLE_COMPUTER,
            new DefaultValueRowValueComputer(columns),
            new ComputedRowValueComputer(columns),
            new VisibilityRowValueComputer(columns));

    toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .toList();
  }

  public void applyValidationAndComputed(Row row) {
    for (Column column : toValidateAndCompute) {
      for (RowValueComputer computer : computers) {
        if (computer.shouldComputeForColumn(column, row)) {
          computer.apply(column, row);
          break;
        }
      }
    }
  }
}
