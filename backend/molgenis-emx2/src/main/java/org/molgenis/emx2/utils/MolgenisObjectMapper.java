package org.molgenis.emx2.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.molgenis.emx2.MolgenisException;

public enum MolgenisObjectMapper {
  INTERNAL {
    @Override
    void configureSpecific(ObjectMapper objectMapper) {
      // Enable source inclusion in error location
      objectMapper.configure(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature(), true);
    }
  },
  PUBLIC;

  // Immutable singletons
  private ObjectReader reader;
  private ObjectWriter writer;

  public ObjectReader getReader() {
    return reader;
  }

  public ObjectWriter getWriter() {
    return writer;
  }

  MolgenisObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    configureGeneric(objectMapper);
    configureSpecific(objectMapper);

    this.reader = objectMapper.reader();
    this.writer = objectMapper.writer();
  }

  private void configureGeneric(ObjectMapper objectMapper) {
    // No trailing data allowed
    objectMapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
    // Duplicate check reduces performance
    objectMapper.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
  }

  /** Override for specific configs. */
  void configureSpecific(ObjectMapper objectMapper) {}

  /**
   * @see #validate(JsonNode)
   */
  public String validate(String json) throws JsonProcessingException {
    return validate(reader.readTree(json)).toString();
  }

  /** Validate JSON if it adheres to Molgenis EMX2 specific requirements. */
  public JsonNode validate(JsonNode rootNode) {
    if (!(rootNode.isObject() || rootNode.isArray())) {
      throw new MolgenisException(
          "Only an object or array is allowed as root element. Found type is: "
              + rootNode.getNodeType().name());
    }
    return rootNode;
  }
}
