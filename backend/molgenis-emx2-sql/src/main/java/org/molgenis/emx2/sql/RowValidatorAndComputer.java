package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.AUTO_ID;

import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.resolvers.*;

public class RowValidatorAndComputer {

  public static final SystemRolePrefixResolver ROLE_COMPUTER = new SystemRolePrefixResolver();

  private final List<RowValueResolver> resolvers;
  private final RowValueResolver defaultResolver;
  private final List<Column> toValidateAndCompute;

  public RowValidatorAndComputer(List<Column> columns) {
    this.resolvers =
        List.of(
            ROLE_COMPUTER,
            new DefaultValueResolver(columns),
            new ComputedExpressionResolver(columns));
    this.defaultResolver = new VisibilityResolver(columns);

    toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .toList();
  }

  public void applyValidationAndComputed(Row row) {
    for (Column column : toValidateAndCompute) {
      resolvers.stream()
          .filter(r -> r.shouldResolveForColumn(column, row))
          .findFirst()
          .orElse(defaultResolver)
          .apply(column, row);
    }
  }
}
