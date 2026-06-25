package org.molgenis.emx2.sql.resolvers;

import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.resolvers.validators.ExpressionValidator;
import org.molgenis.emx2.sql.resolvers.validators.RequiredValidator;

public class VisibilityResolver implements RowValueResolver {

  private final List<Column> columns;

  public VisibilityResolver(List<Column> columns) {
    this.columns = columns;
  }

  @Override
  public void apply(Column column, Row row) {
    Map<String, Object> javascriptContext = JavascriptContextBuilder.fromRow(columns, row);

    if (columnIsVisible(column, javascriptContext)) {
      new RequiredValidator(javascriptContext).apply(column, row);
      new ExpressionValidator(javascriptContext).apply(column, row);
    } else if (column.isReference()) {
      for (Reference ref : column.getReferences()) {
        row.clear(ref.getName());
      }
    } else {
      row.clear(column.getName());
    }
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
