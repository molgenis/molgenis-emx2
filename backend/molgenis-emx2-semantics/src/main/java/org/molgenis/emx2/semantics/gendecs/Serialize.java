package org.molgenis.emx2.semantics.gendecs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Serialize {
  public static String serializeHpo(HpoTerm hpoTerm) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    return gson.toJson(hpoTerm);
  }
}
