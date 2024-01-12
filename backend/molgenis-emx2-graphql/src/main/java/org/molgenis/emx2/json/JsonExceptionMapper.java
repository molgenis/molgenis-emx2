package org.molgenis.emx2.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonExceptionMapper {

  private JsonExceptionMapper() {
    // hide public constructor
  }

  public static String molgenisExceptionToJson(Exception e) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("message", e.getMessage());

    List<Map<String, String>> errorList = new ArrayList<>();
    errorList.add(map);

    Map<String, List> error = new LinkedHashMap<>();
    error.put("errors", errorList);

    try {
      return JsonUtil.getWriter().writeValueAsString(error);
    } catch (JsonProcessingException ex) {
      return "ERROR CONVERSION FAILED " + ex;
    }
  }
}
