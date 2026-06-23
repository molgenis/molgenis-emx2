package org.molgenis.emx2.sql.row.validators;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.row.computers.RowToMapConverter;

public class RequiredRowValidator implements RowValidator {

  private static final RowToMapConverter converter = new RowToMapConverter();

  @Override
  public void apply(List<Column> columns, Row row) throws MolgenisException {
    Map<String, Object> graph = converter.convertRowToMap(columns, row);

    List<Column> toValidateAndCompute =
        columns.stream()
            .filter(c -> !c.isHeading())
            .filter(c -> !AUTO_ID.equals(c.getColumnType()))
            .toList();

    for (Column c : toValidateAndCompute) {

      if (!row.isDraft() && c.getComputed() == null && !AUTO_ID.equals(c.getColumnType())) {
        if (c.isRequired() && hasEmptyFields(c, row)) {
          throw new MolgenisException("column '" + c.getName() + "' is required in " + row);
        } else if (c.isConditionallyRequired()) {
          String error = checkRequiredExpression(c.getRequired(), graph);
          if (error != null && hasEmptyFields(c, row)) {
            throw new MolgenisException(
                "column '" + c.getName() + "' is required: " + error + " in " + row);
          }
        }
      }
      if (c.isReference()) {
        List<Reference> refs = c.getReferences();
        // PostgreSQL considers the foreign key constraint not applicable if any part of the
        // composite
        // key is NULL.therefore we must make sure it is complete
        // exclude overlapping
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
                  c.getRefTableName()));
        }
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

  private String checkRequiredExpression(String validationScript, Map<String, Object> values) {
    try {
      Object error = executeJavascriptOnMap(validationScript, values);
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
