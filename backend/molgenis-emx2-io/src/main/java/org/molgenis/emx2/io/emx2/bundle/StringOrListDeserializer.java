package org.molgenis.emx2.io.emx2.bundle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class StringOrListDeserializer extends StdDeserializer<List<String>> {

  StringOrListDeserializer() {
    super(List.class);
  }

  @Override
  public List<String> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    if (parser.currentToken() == JsonToken.VALUE_STRING) {
      return List.of(parser.getText());
    }
    List<String> result = new ArrayList<>();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      result.add(parser.getText());
    }
    return result;
  }
}
