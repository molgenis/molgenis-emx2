package org.molgenis.emx2.io.emx2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;

public final class ModelSchemaValidator {

  private final JsonSchema schema;
  private final YAMLMapper yamlMapper = new YAMLMapper();

  public ModelSchemaValidator() {
    this(ModelSchemaGenerator.generate());
  }

  public ModelSchemaValidator(String schemaJson) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    this.schema = factory.getSchema(schemaJson);
  }

  public List<String> validate(String yamlContent, String fileLabel) {
    JsonNode document;
    try {
      document = yamlMapper.readTree(yamlContent);
    } catch (Exception exception) {
      return List.of(fileLabel + ": not valid YAML (" + exception.getMessage() + ")");
    }
    if (document == null) {
      return List.of();
    }
    Set<ValidationMessage> messages = schema.validate(document);
    List<String> errors = new ArrayList<>();
    for (ValidationMessage message : messages) {
      errors.add(fileLabel + ": " + message.getMessage());
    }
    return errors;
  }

  public void validateOrThrow(String yamlContent, String fileLabel) {
    List<String> errors = validate(yamlContent, fileLabel);
    if (!errors.isEmpty()) {
      throw new MolgenisException(errors.stream().collect(Collectors.joining("; ")));
    }
  }
}
