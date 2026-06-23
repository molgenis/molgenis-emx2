package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.TypeUtils;

public class ComputedRowValueComputer implements RowValueComputer {

  private static final RowToMapConverter converter = new RowToMapConverter();

  @Override
  public void apply(List<Column> columns, Row row) {
    Map<String, Object> graph = converter.convertRowToMap(columns, row);

    List<Column> toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .filter(c -> c.getComputed() != null)
            .toList();

    for (Column c : toValidateAndCompute) {
      Object computedValue = executeJavascriptOnMap(c.getComputed(), graph);
      TypeUtils.addFieldObjectToRow(c, computedValue, row);
    }
  }
}
