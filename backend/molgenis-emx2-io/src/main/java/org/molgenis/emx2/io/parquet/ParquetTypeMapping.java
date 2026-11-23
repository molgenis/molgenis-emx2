package org.molgenis.emx2.io.parquet;

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
import org.molgenis.emx2.MolgenisException;

final class ParquetTypeMapping {

  /**
   * Values that are supported by the Parquet format.
   *
   * @see <a href="https://parquet.apache.org/docs/file-format/types/">Parquet type docs</a>
   */
  private static final Set<Class<?>> PARQUET_VALUE_TYPES =
      Set.of(
          Boolean.class,
          Integer.class,
          Long.class,
          Double.class,
          LocalDate.class,
          LocalDateTime.class,
          String.class);

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
      Class<?> valueType = valueType(columnType);
      Type.Repetition repetition = Type.Repetition.OPTIONAL;
      if (isRepeated(columnType)) {
        repetition = Type.Repetition.REPEATED;
      }

      switch (valueType.getSimpleName()) {
        case "Boolean" -> schema.primitive(PrimitiveTypeName.BOOLEAN, repetition).named(columnName);
        case "Integer" -> schema.primitive(PrimitiveTypeName.INT32, repetition).named(columnName);
        case "Long" -> schema.primitive(PrimitiveTypeName.INT64, repetition).named(columnName);
        case "Double" -> schema.primitive(PrimitiveTypeName.DOUBLE, repetition).named(columnName);
        case "LocalDate" ->
            schema
                .primitive(PrimitiveTypeName.INT32, repetition)
                .as(LogicalTypeAnnotation.dateType())
                .named(columnName);
        case "LocalDateTime" ->
            schema
                .primitive(PrimitiveTypeName.INT64, repetition)
                .as(LogicalTypeAnnotation.timestampType(false, TimeUnit.MICROS))
                .named(columnName);
        case "String" ->
            schema
                .primitive(PrimitiveTypeName.BINARY, repetition)
                .as(LogicalTypeAnnotation.stringType())
                .named(columnName);
        default ->
            throw new MolgenisException(
                "Cannot map Java type " + valueType.getName() + " to a Parquet type");
      }
    }
    return schema.named("Row");
  }

  static boolean isRepeated(ColumnType columnType) {
    Class<?> javaType = columnType.getType();
    return javaType.isArray() && PARQUET_VALUE_TYPES.contains(javaType.getComponentType());
  }

  /**
   * The Java type of the values written to the field: the element type for repeated fields, the
   * column's own type for single values, and String for everything written as text.
   */
  static Class<?> valueType(ColumnType columnType) {
    Class<?> javaType = columnType.getType();
    if (isRepeated(columnType)) {
      return javaType.getComponentType();
    }

    if (PARQUET_VALUE_TYPES.contains(javaType)) {
      return javaType;
    }
    return String.class;
  }
}
