package org.molgenis.emx2.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.molgenis.emx2.graphql.GraphqlExecutor;

public class JavaScriptBindings {

  private JavaScriptBindings() {}

  private static final String SIMPLE_POST_CLIENT = "simplePostClient";

  private static ProxyExecutable createSimplePostClient(String username) {
    return arguments -> {
      String query = arguments.length > 0 ? arguments[0].asString() : null;
      Map<String, Object> variables = arguments.length > 1 ? asMap(arguments[1]) : new HashMap<>();
      String schemaId =
          arguments.length > 2 && !arguments[2].isNull() ? arguments[2].asString() : null;
      GraphqlExecutor graphQL =
          ApplicationCachePerUser.getInstance().getSchemaGraphqlForUser(schemaId, username);
      return graphQL.executeWithoutSession(query, variables).getData();
    };
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Value value) {
    Object converted = toJava(value);
    return converted instanceof Map ? (Map<String, Object>) converted : new HashMap<>();
  }

  private static Object toJava(Value value) {
    if (value == null || value.isNull()) return null;
    if (value.isBoolean()) return value.asBoolean();
    if (value.isString()) return value.asString();
    if (value.isNumber()) {
      if (value.fitsInInt()) return value.asInt();
      if (value.fitsInLong()) return value.asLong();
      return value.asDouble();
    }
    if (value.hasArrayElements()) {
      List<Object> list = new ArrayList<>();
      for (long i = 0; i < value.getArraySize(); i++) {
        list.add(toJava(value.getArrayElement(i)));
      }
      return list;
    }
    if (value.hasMembers()) {
      Map<String, Object> map = new LinkedHashMap<>();
      for (String key : value.getMemberKeys()) {
        map.put(key, toJava(value.getMember(key)));
      }
      return map;
    }
    return value.toString();
  }

  public static Map<String, Supplier<Object>> getBindingsForUser(String username) {
    Map<String, Supplier<Object>> bindings = new HashMap<>();
    bindings.put(SIMPLE_POST_CLIENT, () -> createSimplePostClient(username));
    // Add more bindings here in a similar way if needed

    return bindings;
  }
}
