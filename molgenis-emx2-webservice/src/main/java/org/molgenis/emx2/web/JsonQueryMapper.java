package org.molgenis.emx2.web;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsoniterSpi;
import org.molgenis.*;

import java.util.*;

import static org.molgenis.Order.ASC;

public class JsonQueryMapper {
  private static class JsonQuery {
    List<String> select = new ArrayList<>();
    List<Map<String, Object[]>> filters = new ArrayList<>();
    Map<String, Order> sort = new LinkedHashMap<>();
  }

  public static Query jsonToQuery(String json, Query q) {
    JsoniterSpi.registerTypeImplementation(Map.class, LinkedHashMap.class);
    JsonQuery jq = JsonIterator.deserialize(json, JsonQuery.class);
    for (String path : jq.select) {
      q.select(path.split("/"));
    }

    for (Map<String, Object[]> or : jq.filters) {
      boolean first = true;
      for (Map.Entry<String, Object[]> f : or.entrySet()) {
        String path = f.getKey();
        Object[] value = f.getValue();
        Where where;
        if (first) {
          where = q.or(path.split("/"));
          first = false;
        } else {
          where = q.and(path.split("/"));
        }
        if (value[0] instanceof String) {
          where.eq(Arrays.copyOf(value, value.length, String[].class));
        }
        if (value[0] instanceof Double) {
          where.eq(Arrays.copyOf(value, value.length, Double[].class));
        }
        if (value[0] instanceof Integer) {
          where.eq(Arrays.copyOf(value, value.length, Integer[].class));
        }
      }
    }
    for (Map.Entry<String, Order> s : jq.sort.entrySet()) {
      if (ASC.equals(s.getValue())) q.asc(s.getKey());
      else q.desc(s.getKey());
    }
    return q;
  }

  public static String queryToJson(Query q) {
    JsonQuery result = new JsonQuery();

    for (Select s : q.getSelectList()) {
      result.select.add(String.join("/", s.getPath()));
    }

    Map<String, Object[]> clause = new LinkedHashMap<>();
    result.filters.add(clause);
    for (Where f : q.getWhereLists()) {
      Operator op = f.getOperator();
      if (Operator.OR.equals(op)) {
        clause = new LinkedHashMap<>();
        result.filters.add(clause);
      } else if (Operator.EQ.equals(op)) clause.put(String.join("/", f.getPath()), f.getValues());
    }

    for (Sort s : q.getSortList()) {
      result.sort.put(String.join("/", s.getPath()), s.getOrder());
    }

    return JsonStream.serialize(result);
  }
}
