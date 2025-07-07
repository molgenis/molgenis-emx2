package org.molgenis.emx2.rdf.shacl;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;

public abstract class ShaclSelector {
  private static final ClassLoader classLoader = ShaclSelector.class.getClassLoader();
  private static final Map<String, ShaclSet> shaclSetMap = new HashMap<>();

  static {
    // todo: fix "Could not initialize class org.molgenis.emx2.rdf.shacl.ShaclSelector"
    update();
  }

  public static void update() {
    shaclSetMap.clear();

    ShaclSet[] ShaclSetArray;
    try (InputStream inputStream = classLoader.getResourceAsStream("_shacl/sets.yaml")) {
      String yaml = new String(Objects.requireNonNull(inputStream).readAllBytes());
      ShaclSetArray =
          JsonMapper.builder(new YAMLFactory()).build().readValue(yaml, ShaclSet[].class);
    } catch (IOException e) {
      throw new MolgenisException("An error occured while loading the shacl sets: " + e);
    }

    for (ShaclSet shaclSet : ShaclSetArray) {
      shaclSetMap.put(shaclSet.name(), shaclSet);
    }
  }

  public static Set<String> getNames() {
    return shaclSetMap.keySet();
  }

  public static ShaclSet get(String name) {
    return shaclSetMap.get(name);
  }
}
