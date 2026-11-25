package org.molgenis.emx2.io.parquet;

import static org.molgenis.emx2.ColumnType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.molgenis.emx2.ColumnType;

final class ParquetTypeMapping {

  private static final Set<ColumnType> REPEATED_TYPES =
      Set.of(
          BOOL_ARRAY,
          INT_ARRAY,
          LONG_ARRAY,
          DECIMAL_ARRAY,
          DATE_ARRAY,
          DATETIME_ARRAY,
          STRING_ARRAY,
          TEXT_ARRAY);

  private ParquetTypeMapping() {
    // Hide public constructor
  }

  /**
   * Builds the file schema: one column per entry of {@code columnTypes}, in its iteration order.
   * The file schema is used to communicate the format of the rows and columns to Parquet. The
   * fields are written in the same order as {@code columnTypes}.
   */
  static MessageType schema(SequencedMap<String, ColumnType> columnTypes) {
    Types.MessageTypeBuilder schema = Types.buildMessage();

    for (Map.Entry<String, ColumnType> column : columnTypes.entrySet()) {
      String columnName = column.getKey();
      ColumnType columnType = column.getValue();
      Type.Repetition repetition =
          isRepeated(columnType) ? Type.Repetition.REPEATED : Type.Repetition.OPTIONAL;

      switch (columnType.getBaseType()) {
        case BOOL, BOOL_ARRAY ->
            schema.primitive(PrimitiveTypeName.BOOLEAN, repetition).named(columnName);
        case INT, INT_ARRAY ->
            schema.primitive(PrimitiveTypeName.INT32, repetition).named(columnName);
        case LONG, LONG_ARRAY ->
            schema.primitive(PrimitiveTypeName.INT64, repetition).named(columnName);
        case DECIMAL, DECIMAL_ARRAY ->
            schema.primitive(PrimitiveTypeName.DOUBLE, repetition).named(columnName);
        case DATE, DATE_ARRAY ->
            schema
                .primitive(PrimitiveTypeName.INT32, repetition)
                .as(LogicalTypeAnnotation.dateType())
                .named(columnName);
        case DATETIME, DATETIME_ARRAY ->
            schema
                .primitive(PrimitiveTypeName.INT64, repetition)
                .as(LogicalTypeAnnotation.timestampType(false, TimeUnit.MILLIS))
                .named(columnName);
        default ->
            schema
                .primitive(PrimitiveTypeName.BINARY, repetition)
                .as(LogicalTypeAnnotation.stringType())
                .named(columnName);
      }
    }
    return schema.named("Row");
  }

  static boolean isRepeated(ColumnType columnType) {
    return REPEATED_TYPES.contains(columnType.getBaseType());
  }

  /**
   * The Java type of the values written to the field: the element type for repeated fields, the
   * column's own type for single values, and String for everything written as text.
   */
  static Class<?> valueType(ColumnType columnType) {
    return switch (columnType.getBaseType()) {
      case BOOL, BOOL_ARRAY -> Boolean.class;
      case INT, INT_ARRAY -> Integer.class;
      case LONG, LONG_ARRAY -> Long.class;
      case DECIMAL, DECIMAL_ARRAY -> Double.class;
      case DATE, DATE_ARRAY -> LocalDate.class;
      case DATETIME, DATETIME_ARRAY -> LocalDateTime.class;
      default -> String.class;
    };
  }
}
