package org.molgenis.emx2.fairmapper.client;

import java.util.HashMap;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.SchemaMetadataProvider;

public class CachingSchemaMetadataProvider implements SchemaMetadataProvider {

  private final SchemaMetadataProvider delegate;

  private final HashMap<String, SchemaMetadata> cache = new HashMap<>();

  public CachingSchemaMetadataProvider(SchemaMetadataProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public SchemaMetadata getSchemaMetadata(String schemaName) {
    return cache.computeIfAbsent(schemaName, delegate::getSchemaMetadata);
  }
}
