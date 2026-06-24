package org.molgenis.emx2.sql.resolvers.validators;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;

public class RequiredValidator implements RowValidator {

  private final Map<String, Object> context;

  public RequiredValidator(Map<String, Object> context) {
    this.context = context;
  }

  @Override
  public void apply(Column column, Row row) throws MolgenisException {
    if (!row.isDraft() && column.getComputed() == null && !AUTO_ID.equals(column.getColumnType())) {
      if (column.isRequired() && hasEmptyFields(column, row)) {
        throw new MolgenisException("column '" + column.getName() + "' is required in " + row);
      } else if (column.isConditionallyRequired()) {
        String error = checkRequiredExpression(column.getRequired());
        if (error != null && hasEmptyFields(column, row)) {
          throw new MolgenisException(
              "column '" + column.getName() + "' is required: " + error + " in " + row);
        }
      }
    }
    if (column.isReference()) {
      List<Reference> refs = column.getReferences();
      int countNotNullNotOverlapping = 0;
      int countNotNull = 0;
      for (Reference ref : refs) {
        if (!row.isNull(ref.getName(), ref.getPrimitiveType())) {
          if (!ref.isOverlapping()) {
            countNotNullNotOverlapping++;
          }
          countNotNull++;
        }
      }
      if (countNotNullNotOverlapping > 0 && countNotNull != refs.size()) {
        throw new MolgenisException(
            String.format(
                "Key (%s)=(%s) not present in table \"%s\"",
                refs.stream().map(ref -> ref.getName()).collect(Collectors.joining(",")),
                refs.stream()
                    .map(
                        ref ->
                            row.isNull(ref.getName(), ref.getPrimitiveType())
                                ? "NULL"
                                : row.getValueMap().get(ref.getName()).toString())
                    .collect(Collectors.joining(",")),
                column.getRefTableName()));
      }
    }
  }

  private boolean hasEmptyFields(Column c, Row row) {
    if (c.isReference()) {
      for (Reference r : c.getReferences()) {
        if (row.isNull(r.getName(), r.getPrimitiveType())) {
          return true;
        }
      }
    } else {
      return row.isNull(c.getName(), c.getColumnType());
    }
    return false;
  }

  private String checkRequiredExpression(String validationScript) {
    try {
      Object error = executeJavascriptOnMap(validationScript, context);
      if (error instanceof Boolean) {
        if ((Boolean) error) return validationScript;
        return null;
      }
      if (error != null) return error.toString();
    } catch (MolgenisException me) {
      throw me;
    }
    return null;
  }
}
