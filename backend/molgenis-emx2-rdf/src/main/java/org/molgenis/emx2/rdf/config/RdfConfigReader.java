package org.molgenis.emx2.rdf.config;

import static org.molgenis.emx2.rdf.RdfUtils.SETTING_RDF_CONFIG;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

public abstract class RdfConfigReader {
  public static RdfConfig read(Schema schema) {
    if (schema.hasSetting(SETTING_RDF_CONFIG)) {
      String value = schema.getSettingValue(SETTING_RDF_CONFIG);
      if (value.isEmpty()) return RdfConfig.getDefaults();
      return processSetting(value);
    }
    return RdfConfig.getDefaults();
  }

  private static RdfConfig processSetting(String value) {
    try {
      return JsonMapper.builder(new YAMLFactory())
          .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
          .build()
          .readValue(value, RdfConfig.class);
    } catch (JsonProcessingException e) {
      throw new MolgenisException("An error occurred while loading the RDF configuration: " + e);
    }
  }
}
