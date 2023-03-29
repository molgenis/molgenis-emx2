package org.molgenis.emx2.io.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class YamlSchemaValidator {
  public static void validate(JsonNode input) throws IOException {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    JsonSchema jsonSchema =
        factory.getSchema(
            YamlSchemaValidator.class.getResourceAsStream("emx2_yaml_format_schema.json"));
    Set<ValidationMessage> errors = jsonSchema.validate(input);
    if (errors.size() > 0) {
      throw new IOException(
          errors.stream().map(message -> message.getMessage()).collect(Collectors.joining(".\n")));
    }
  }
}
