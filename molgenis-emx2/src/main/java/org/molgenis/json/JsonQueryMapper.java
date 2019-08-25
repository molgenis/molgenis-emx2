package org.molgenis.json;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsoniterSpi;
import org.molgenis.query.*;

import java.util.*;

import static org.molgenis.query.Order.ASC;

public class JsonQueryMapper {

  private JsonQueryMapper() {
    // hide constructor
  }

  private static class JsonQuery {
    List<String> select = new ArrayList<>();
    List<Map<String, Object[]>> filters = new ArrayList<>();
    Map<String, Order> sort = new LinkedHashMap<>();
  }

  public static Query jsonToQuery(String json, Query q) {
    JsoniterSpi.registerTypeImplementation(Map.class, LinkedHashMap.class);
    JsonQuery jq = JsonIterator.deserialize(json, JsonQuery.class);
    selectToQuery(q, jq);
    filtersToQuery(q, jq);
    sortToQuery(q, jq);
    return q;
  }

  public static void selectToQuery(Query q, JsonQuery jq) {
    for (String path : jq.select) {
      q.select(path.split("/"));
    }
  }

  private static void sortToQuery(Query q, JsonQuery jq) {
    for (Map.Entry<String, Order> s : jq.sort.entrySet()) {
      if (ASC.equals(s.getValue())) q.asc(s.getKey());
      else q.desc(s.getKey());
    }
  }

  public static void filtersToQuery(Query q, JsonQuery jq) {
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
        where.eq(Arrays.copyOf(value, value.length, Object[].class));
      }
    }
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
