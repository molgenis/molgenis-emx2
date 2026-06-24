package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.AUTO_ID;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.resolvers.*;

public class RowValidatorAndComputer {

  public static final SystemRolePrefixResolver ROLE_COMPUTER = new SystemRolePrefixResolver();

  private final List<RowValueResolver> resolvers;
  private final List<Column> toValidateAndCompute;

  public RowValidatorAndComputer(List<Column> columns) {
    this.resolvers =
        List.of(
            ROLE_COMPUTER,
            new DefaultValueResolver(columns),
            new ComputedExpressionResolver(columns),
            new VisibilityResolver(columns));

    toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .toList();
  }

  public void applyValidationAndComputed(Row row) {
    for (Column column : toValidateAndCompute) {
      for (RowValueResolver resolver : resolvers) {
        if (resolver.shouldResolveForColumn(column, row)) {
          resolver.apply(column, row);
          break;
        }
      }
    }
  }
}
