package org.molgenis.emx2.semantics.gendecs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;

public class Serialize {
  public static String serializeHpo(HpoTerm hpoTerm) {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    return gson.toJson(hpoTerm);
  }

  public static String serialzeVariants(ArrayList<Variant> variants) {
    Gson gson = new Gson();
    return gson.toJson(variants);
  }
}
