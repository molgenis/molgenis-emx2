package org.molgenis.beans;

import org.molgenis.Row;
import org.molgenis.Column;
import org.molgenis.TableMetadata;
import org.molgenis.utils.MolgenisException;
import org.molgenis.utils.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Mapper {

  private Mapper() {
    // hides public constructor
  }

  public static Row[] map(Object... beans) throws MolgenisException {
    ArrayList<Row> rows = new ArrayList<>();
    try {
      for (Object b : beans) {
        Class c = b.getClass();
        Method[] methods = c.getDeclaredMethods();
        Map<String, Object> values = new LinkedHashMap<>();

        for (Method m : methods) {
          if (m.getName().startsWith("get") && m.getParameterCount() == 0) {
            values.put(m.getName().substring(3), m.invoke(b));
          }
        }

        rows.add(new Row(values));
      }
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
    return rows.toArray(new Row[rows.size()]);
  }

  public static <E> E map(Class<E> klazz, Row row) throws MolgenisException {
    try {
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
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
  }

  public static TableMetadata map(Class klazz) throws MolgenisException {
    TableMetadata t = new TableMetadata(null, klazz.getSimpleName());

    Field[] fields = klazz.getDeclaredFields();
    for (Field f : fields) {
      try {
        if (!f.getName().contains("jacoco")) {
          Column col = t.addColumn(f.getName(), TypeUtils.typeOf(f.getType()));
          if (f.isAnnotationPresent(ColumnAnnotation.class)) {
            ColumnAnnotation cm = f.getAnnotation(ColumnAnnotation.class);
            col.setNullable(cm.nullable());
            col.setDescription(cm.description());
          }
        }
      } catch (Exception e) {
        throw new MolgenisException("Failed to map field " + f.getName(), e);
      }
    }
    return t;
  }
}
