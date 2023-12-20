package org.molgenis.emx2.beans;

import static org.molgenis.emx2.Column.column;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.TypeUtils;

public class Mapper {

  private Mapper() {
    // hides public constructor
  }

  public static Row[] map(Object... beans)
      throws InvocationTargetException, IllegalAccessException {
    ArrayList<Row> rows = new ArrayList<>();
    for (Object b : beans) {
      Class<?> c = b.getClass();
      Method[] methods = c.getDeclaredMethods();
      Map<String, Object> values = new LinkedHashMap<>();

      for (Method m : methods) {
        if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
          values.put(m.getName().substring(3), m.invoke(b));
        }
      }

      rows.add(new Row(values));
    }
    return rows.toArray(new Row[rows.size()]);
  }

  public static <E> E map(Class<E> klazz, Row row)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
          InstantiationException {
    E e = klazz.getConstructor().newInstance();
    Map<String, Object> values = row.getValueMap();
    for (String name : row.getColumnNames()) {
      Object value = values.get(name);
      if (value != null) {
        Method m =
            klazz.getMethod(
                "set" + name.substring(0, 1).toUpperCase() + name.substring(1), value.getClass());
        m.invoke(e, values.get(name));
      }
    }
    return e;
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
        }
        t.add(col);
      }
    }
    return t;
  }
}
