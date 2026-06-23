package org.molgenis.emx2.sql;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.row.computers.ComputedRowValueComputer;
import org.molgenis.emx2.sql.row.computers.DefaultValueRowValueComputer;
import org.molgenis.emx2.sql.row.computers.RowValueComputer;
import org.molgenis.emx2.sql.row.computers.SystemEditRolePrefixRowValueComputer;
import org.molgenis.emx2.sql.row.validators.RequiredRowValidator;
import org.molgenis.emx2.sql.row.validators.RowValidator;
import org.molgenis.emx2.sql.row.validators.ValidationRowValidator;

public class RowValidatorAndComputer {

  private static final List<RowValueComputer> COMPUTERS =
      List.of(
          new DefaultValueRowValueComputer(),
          new ComputedRowValueComputer(),
          new SystemEditRolePrefixRowValueComputer());

  private static final List<RowValidator> VALIDATORS =
      List.of(new RequiredRowValidator(), new ValidationRowValidator());

  public void applyValidationAndComputed(List<Column> columns, Row row) {
    for (RowValueComputer computer : COMPUTERS) {
      computer.apply(columns, row);
    }

    for (RowValidator validator : VALIDATORS) {
      validator.apply(columns, row);
    }
  }
}
