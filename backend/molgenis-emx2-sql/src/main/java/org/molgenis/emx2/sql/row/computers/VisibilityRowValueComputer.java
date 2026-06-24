package org.molgenis.emx2.sql.row.computers;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.row.computers.validators.RequiredRowValidator;
import org.molgenis.emx2.sql.row.computers.validators.ValidationRowValidator;

public class VisibilityRowValueComputer implements RowValueComputer {

  private final List<Column> columns;

  public VisibilityRowValueComputer(List<Column> columns) {
    this.columns = columns;
  }

  @Override
  public void apply(Column column, Row row) {
    Map<String, Object> javascriptContext = JavascriptContextBuilder.fromRow(columns, row);

    if (columnIsVisible(column, javascriptContext)) {
      new RequiredRowValidator(javascriptContext).apply(column, row);
      new ValidationRowValidator(javascriptContext).apply(column, row);
    } else if (column.isReference()) {
      for (Reference ref : column.getReferences()) {
        row.clear(ref.getName());
      }
    } else {
      row.clear(column.getName());
    }
  }

  @Override
  public boolean shouldComputeForColumn(Column column, Row row) {
    return true;
  }

  private static boolean columnIsVisible(Column column, Map values) {
    if (column.getVisible() != null) {
      Object visibleResult = executeJavascriptOnMap(column.getVisible(), values);
      if (visibleResult == null || Boolean.FALSE.equals(visibleResult)) {
        return false;
      }
    }
    return true;
  }
}
