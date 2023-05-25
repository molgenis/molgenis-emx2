package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.AUTO_ID;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.utils.generator.IdGenerator;
import org.molgenis.emx2.utils.generator.IdGeneratorImpl;

public class SqlTypeUtils extends TypeUtils {

  public static final IdGenerator idGenerator = new IdGeneratorImpl();

  private SqlTypeUtils() {
    // to hide the public constructor
  }

  static Map<String, Object> validateAndGetVisibleValuesAsMap(
      Row row, TableMetadata tableMetadata, Collection<Column> updateColumns) {

    // we create a graph representation of the row, using identifiers
    Map<String, Object> graph = convertRowToMap(tableMetadata, row);
    Set<String> colNames =
        updateColumns.stream()
            .map(
                c ->
                    // oldName is also used for references so we can know the underlying column name
                    c.getOldName() != null ? c.getOldName() : c.getName())
            .collect(Collectors.toSet());
    // only validate columns for which we have data
    Set<Column> validationColumns =
        tableMetadata.getColumns().stream()
            .filter(c2 -> colNames.contains(c2.getName()))
            .collect(Collectors.toSet());

    // we null all invisible columns
    for (Column c : validationColumns) {
      if (!columnIsVisible(c, graph)) {
        graph.put(c.getName(), null);
      }
    }

    // then we validate
    for (Column c : validationColumns) {
      checkValidation(c, graph);
    }

    // if passed validation we gonna create update record
    Map<String, Object> values = new LinkedHashMap<>();
    for (Column c : updateColumns) {
      // we get role from environment
      if (Constants.MG_EDIT_ROLE.equals(c.getName())) {
        values.put(c.getName(), Constants.MG_USER_PREFIX + row.getString(Constants.MG_EDIT_ROLE));
      }
      // autoid field
      else if (AUTO_ID.equals(c.getColumnType())) {
        // do we use a template containing ${mg_autoid} for pre/postfixing ?
        if (c.getComputed() != null && row.isNull(c.getName(), c.getPrimitiveColumnType())) {
          values.put(
              c.getName(),
              c.getComputed().replace(Constants.COMPUTED_AUTOID_TOKEN, idGenerator.generateId()));
        }
        // otherwise simply put the id
        else if (row.isNull(c.getName(), c.getPrimitiveColumnType())) {
          values.put(c.getName(), idGenerator.generateId());
        } else {
          values.put(c.getName(), getTypedValue(c, row));
        }
      }
      // compute field, might depend on update values therefor run always on insert/update
      else if (c.getComputed() != null) {
        values.put(c.getName(), executeJavascriptOnMap(c.getComputed(), graph));
      }
      // otherwise, unless invisible
      else if (columnIsVisible(
          c.getOldName() != null ? tableMetadata.getColumn(c.getOldName()) : c, graph)) {
        values.put(c.getName(), getTypedValue(c, row));
      }
      // invisible columns will be in updatedColumns so set to null
      else {
        values.put(c.getName(), null);
      }
    }
    return values;
  }

  private static boolean columnIsVisible(Column column, Map values) {
    if (column.getVisible() != null) {
      String visibleResult = executeJavascriptOnMap(column.getVisible(), values);
      if (visibleResult.equals("false") || visibleResult.equals("null")) {
        return false;
      }
    }
    return true;
  }

  public static Object getTypedValue(Column c, Row row) {
    String name = c.getName();
    return switch (c.getPrimitiveColumnType()) {
      case FILE -> row.getBinary(name);
      case UUID -> row.getUuid(name);
      case UUID_ARRAY -> row.getUuidArray(name);
      case STRING, EMAIL, HYPERLINK -> row.getString(name);
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY -> row.getStringArray(name);
      case BOOL -> row.getBoolean(name);
      case BOOL_ARRAY -> row.getBooleanArray(name);
      case INT -> row.getInteger(name);
      case INT_ARRAY -> row.getIntegerArray(name);
      case LONG -> row.getLong(name);
      case LONG_ARRAY -> row.getLongArray(name);
      case DECIMAL -> row.getDecimal(name);
      case DECIMAL_ARRAY -> row.getDecimalArray(name);
      case TEXT -> row.getText(name);
      case TEXT_ARRAY -> row.getTextArray(name);
      case DATE -> row.getDate(name);
      case DATE_ARRAY -> row.getDateArray(name);
      case DATETIME -> row.getDateTime(name);
      case DATETIME_ARRAY -> row.getDateTimeArray(name);
      case JSONB -> row.getJsonb(name);
      case JSONB_ARRAY -> row.getJsonbArray(name);
      default -> throw new UnsupportedOperationException(
          "Unsupported columnType found:" + c.getColumnType());
    };
  }

  static String getPsqlType(Column column) {
    return getPsqlType(column.getPrimitiveColumnType());
  }

  static String getPsqlType(ColumnType type) {
    return switch (type) {
      case STRING, EMAIL, HYPERLINK, TEXT -> "character varying";
      case STRING_ARRAY, EMAIL_ARRAY, HYPERLINK_ARRAY, TEXT_ARRAY -> "character varying[]";
      case UUID -> "uuid";
      case UUID_ARRAY -> "uuid[]";
      case BOOL -> "bool";
      case BOOL_ARRAY -> "bool[]";
      case INT -> "int";
      case INT_ARRAY -> "int[]";
      case LONG -> "bigint";
      case LONG_ARRAY -> "bigint[]";
      case DECIMAL -> "decimal";
      case DECIMAL_ARRAY -> "decimal[]";
      case DATE -> "date";
      case DATE_ARRAY -> "date[]";
      case DATETIME -> "timestamp without time zone";
      case DATETIME_ARRAY -> "timestamp without time zone[]";
      case JSONB -> "jsonb";
      default -> throw new MolgenisException(
          "Unknown type: Internal error: data cannot be mapped to psqlType " + type);
    };
  }

  public static void checkValidation(Column column, Map<String, Object> values) {
    if (values.get(column.getName()) != null) {
      column.getColumnType().validate(values.get(column.getName()));
      // validation
      if (column.getValidation() != null) {
        String errorMessage = checkValidation(column.getValidation(), values);
        if (errorMessage != null)
          throw new MolgenisException(
              "Validation error on column '" + column.getName() + "': " + errorMessage + ".");
      }
    }
  }

  public static String checkValidation(String validationScript, Map<String, Object> values) {
    try {
      String error = executeJavascriptOnMap(validationScript, values);
      if (error != null) {
        if (error.trim().equals("false")) {
          // you can have a validation rule that simply returns true or false;
          // false means not valid.
          return validationScript;
        } else
        // you can have a validation script returning true which means valid, and undefined also
        if (!error.trim().equals("true") && !error.trim().equals("undefined")) {
          return error;
        }
      }
      return null;
    } catch (MolgenisException me) {
      // seperate syntax errors
      throw me;
    }
  }

  static Map<String, Object> convertRowToMap(TableMetadata tableMetadata, Row row) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Column c : tableMetadata.getColumns()) {
      if (c.isReference()) {
        map.put(c.getIdentifier(), getRefFromRow(row, c));
      } else if (c.isFile()) {
        // skip file
      } else {
        map.put(c.getIdentifier(), row.get(c));
      }
    }
    return map;
  }

  static Object getRefFromRow(Row row, Column c) {
    if (c.isRefArray() || c.isRefback()) {
      List<Map> result = new ArrayList<>();
      for (Reference ref : c.getReferences()) {
        if (!ref.isOverlapping()) {
          // must be a list
          if (row.getValueMap().get(ref.getName()) != null) {
            int i = 0;
            for (Object value : (Object[]) row.get(ref.getName(), ref.getPrimitiveType())) {
              if (i == result.size()) {
                result.add(new LinkedHashMap<>());
              }
              putMap(result.get(i), ref.getPath(), value);
              i++;
            }
          }
        }
      }
      return result;
    } else {
      // returns a value
      Map<String, Object> result = new LinkedHashMap<>();
      for (Reference ref : c.getReferences()) {
        if (!ref.isOverlapping()) {
          List<String> path = new ArrayList();
          path.add(c.getIdentifier());
          path.addAll(ref.getPath());
          putMap(result, ref.getPath(), row.get(ref.getName(), ref.getPrimitiveType()));
        }
      }
      return result;
    }
  }

  // put map hierarchy
  private static void putMap(Map<String, Object> result, List<String> path, Object value) {
    if (path.size() == 1) {
      result.put(path.get(0), value);
    } else {
      if (result.get(path.get(0)) == null) {
        result.put(path.get(0), new LinkedHashMap<>());
      }
      putMap((Map) result.get(path.get(0)), path.subList(1, path.size()), value);
    }
  }
}
