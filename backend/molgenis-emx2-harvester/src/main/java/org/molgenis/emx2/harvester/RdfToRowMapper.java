package org.molgenis.emx2.harvester;

import static java.util.function.Predicate.not;

import java.util.Optional;
import org.eclipse.rdf4j.model.Value;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

public class RdfToRowMapper {

  public static Optional<Object> mapToColumnType(Value value, Column column) {
    if (value == null) {
      return Optional.empty();
    }

    switch (column.getColumnType()) {
      case BOOL -> {
        return Optional.of(Boolean.parseBoolean(value.stringValue()));
      }
      case STRING_ARRAY, ONTOLOGY_ARRAY, BOOL_ARRAY -> {
        return Optional.ofNullable(value.stringValue())
            .filter(not(String::isBlank))
            .map(Object.class::cast);
      }
      case STRING, TEXT, TEXT_ARRAY, AUTO_ID, RADIO, HYPERLINK, DATETIME -> {
        return Optional.ofNullable(value.stringValue());
      }
        //      case DATETIME -> {
        //        if (value)
        //      }
      case INT, NON_NEGATIVE_INT -> {
        return Optional.of(Integer.parseInt(value.stringValue()));
      }
      case LONG -> {
        return Optional.of(Long.parseLong(value.stringValue()));
      }
      case DECIMAL -> {
        return Optional.of(Double.parseDouble(value.stringValue()));
      }
      case UUID,
          UUID_ARRAY,
          FILE,
          JSON,
          INT_ARRAY,
          LONG_ARRAY,
          DECIMAL_ARRAY,
          DATE,
          DATE_ARRAY,
          DATETIME_ARRAY,
          PERIOD,
          PERIOD_ARRAY,
          REF,
          REF_ARRAY,
          REFBACK,
          HEADING,
          SECTION,
          ONTOLOGY,
          EMAIL,
          EMAIL_ARRAY,
          HYPERLINK_ARRAY,
          NON_NEGATIVE_INT_ARRAY,
          SELECT,
          MULTISELECT,
          CHECKBOX -> {
        System.out.println("Missing column type: " + column.getColumnType());
        throw new MolgenisException("Add this one: " + column.getColumnType());
      }
      default -> throw new MolgenisException("Unsupported column type: " + column.getColumnType());
    }
  }
}
