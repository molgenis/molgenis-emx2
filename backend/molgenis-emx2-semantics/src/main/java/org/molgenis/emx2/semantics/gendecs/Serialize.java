package org.molgenis.emx2.semantics.gendecs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;

public class Serialize {
  public static String serializeHpo(HpoTerm hpoTerm) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    return gson.toJson(hpoTerm);
  }

  public static String serializeMap(HashMap<String, String> map) {
    Gson gson = new Gson();

    return gson.toJson(map);
  }
}
