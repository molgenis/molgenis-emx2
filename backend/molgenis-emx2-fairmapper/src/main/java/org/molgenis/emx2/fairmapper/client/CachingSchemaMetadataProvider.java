package org.molgenis.emx2.fairmapper.client;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SchemaMetadataProvider;

public class CachingSchemaMetadataProvider implements SchemaMetadataProvider {

  private final SchemaMetadataProvider delegate;

  private final Map<String, SchemaMetadata> cache = new HashMap<>();

  public CachingSchemaMetadataProvider(SchemaMetadataProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public SchemaMetadata getSchemaMetadata(String schemaName) {
    SchemaMetadata schemaMetadata = cache.computeIfAbsent(schemaName, delegate::getSchemaMetadata);
    schemaMetadata.setSchemaMetadataProvider(this);
    return schemaMetadata;
  }
}
