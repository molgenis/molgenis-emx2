package org.molgenis.emx2.beans;

import static java.lang.String.format;
import static org.molgenis.emx2.Column.column;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.TypeUtils;

public class Mapper {

  private Mapper() {
    // hides public constructor
  }

  public static Row[] map(Object... beans) {
    ArrayList<Row> rows = new ArrayList<>();
    for (Object object : beans) {
      Class<?> c = object.getClass();
      Method[] methods = c.getMethods();
      Map<String, Object> values = new LinkedHashMap<>();

      Arrays.stream(methods)
          .filter(m -> m.getName().startsWith("get"))
          .filter(m -> !m.getName().equals("getClass"))
          .filter(m -> m.getParameterCount() == 0)
          .forEach(m -> values.put(setterNameToColumnName(m), invoke(m, object)));

      rows.add(new Row(values));
    }
    return rows.toArray(new Row[0]);
  }

  public static <E> E map(Class<E> klazz, Row row) {
    E object = construct(klazz);
    Method[] methods = klazz.getMethods();
    Map<String, Object> values = row.getValueMap();
    Arrays.stream(methods)
        .filter(m -> m.getName().startsWith("set"))
        .filter(m -> m.getParameterCount() == 1)
        .forEach(
            m -> {
              var fieldName = setterNameToColumnName(m);
              var value = mapColumn(m, values.get(fieldName));
              invoke(m, object, value);
            });

    return object;
  }

  private static String lowerFirstChar(String string) {
    return Character.toLowerCase(string.charAt(0)) + string.substring(1);
  }

  private static String setterNameToColumnName(Method method) {
    return lowerFirstChar(method.getName().substring(3));
  }

  private static Object mapColumn(Method method, Object value) {
    var parameterType = method.getParameters()[0].getType();
    if (parameterType != null && parameterType.isEnum()) {
      mapEnumColumn(method, value, parameterType);
    }
    return value;
  }

  private static void mapEnumColumn(Method method, Object value, Class<?> enumType) {
    try {
      Method valueOf = enumType.getMethod("valueOf", String.class);
      invoke(valueOf, null, value.toString());
    } catch (NoSuchMethodException e) {
      throw new MolgenisException(
          format("Error mapping string to Enum: %s", setterNameToColumnName(method)), e);
    }
  }

  private static <E> E construct(Class<E> cls) {
    try {
      return cls.getConstructor().newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new MolgenisException(format("Error instantiating object: %s", cls.getName()), e);
    }
  }

  private static Object invoke(Method method, Object object, Object... values) {
    try {
        return method.invoke(object, values);
    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
      throw new MolgenisException(
          format("Error calling %s.%s", method.getDeclaringClass().getName(), method.getName()), e);
    }
  }

  public static TableMetadata map(Class<?> klazz) {
    TableMetadata t = new TableMetadata(klazz.getSimpleName());

    Field[] fields = klazz.getDeclaredFields();
    for (Field f : fields) {
      if (!f.getName().contains("jacoco")) {
        Column col = column(f.getName()).setType(TypeUtils.typeOf(f.getType()));
        if (f.isAnnotationPresent(ColumnAnnotation.class)) {
          ColumnAnnotation cm = f.getAnnotation(ColumnAnnotation.class);
          col.setRequired(cm.required());
          col.setDescription(cm.description());
        }
        t.add(col);
      }
    }
    return t;
  }
}
