package org.molgenis.emx2.harvester;

import static java.util.function.Predicate.not;

import java.util.Optional;
import org.eclipse.rdf4j.model.Value;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

public class RdfToRowValueMapper {

  public static Optional<Object> mapToColumnType(Value value, Column column) {
    switch (column.getColumnType()) {
      case BOOL -> {
        return Optional.of(Boolean.parseBoolean(value.stringValue()));
      }
      case UUID -> {}
      case UUID_ARRAY -> {}
      case FILE -> {}
      case STRING_ARRAY, ONTOLOGY_ARRAY, BOOL_ARRAY -> {
        return Optional.ofNullable(value.stringValue())
            .filter(not(String::isBlank))
            .map(Object.class::cast);
      }
      case STRING, TEXT, TEXT_ARRAY, AUTO_ID, RADIO -> {
        return Optional.ofNullable(value.stringValue());
      }
      case JSON -> {}
      case INT -> {
        return Optional.of(Integer.parseInt(value.stringValue()));
      }
      case INT_ARRAY -> {}
      case LONG -> {
        return Optional.of(Long.parseLong(value.stringValue()));
      }
      case LONG_ARRAY -> {}
      case DECIMAL -> {
        return Optional.of(Double.parseDouble(value.stringValue()));
      }
      case DECIMAL_ARRAY -> {}
      case DATE -> {}
      case DATE_ARRAY -> {}
      case DATETIME -> {}
      case DATETIME_ARRAY -> {}
      case PERIOD -> {}
      case PERIOD_ARRAY -> {}
      case REF -> {}
      case REF_ARRAY -> {}
      case REFBACK -> {}
      case HEADING -> {}
      case SECTION -> {}
      case ONTOLOGY -> {}
      case EMAIL -> {}
      case EMAIL_ARRAY -> {}
      case HYPERLINK -> {}
      case HYPERLINK_ARRAY -> {}
      case NON_NEGATIVE_INT -> {}
      case NON_NEGATIVE_INT_ARRAY -> {}
      case SELECT -> {}
      case MULTISELECT -> {}
      case CHECKBOX -> {}
      default -> throw new MolgenisException("Unsupported column type: " + column.getColumnType());
    }

    System.out.println("Missing column type: " + column.getColumnType());
    return Optional.empty();
  }
}
