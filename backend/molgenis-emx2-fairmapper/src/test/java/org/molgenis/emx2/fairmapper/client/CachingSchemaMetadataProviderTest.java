package org.molgenis.emx2.fairmapper.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;

class CachingSchemaMetadataProviderTest {

  @Test
  void shouldCacheSchemaMetadata() {
    CachingSchemaMetadataProvider provider =
        new CachingSchemaMetadataProvider(schemaName -> new SchemaMetadata());
    SchemaMetadata schemaMetadata = provider.getSchemaMetadata("foo");
    assertSame(schemaMetadata, provider.getSchemaMetadata("foo"));
    assertNotSame(schemaMetadata, provider.getSchemaMetadata("bar"));
  }

  @Test
  void shouldSetProvider() {
    CachingSchemaMetadataProvider provider =
        new CachingSchemaMetadataProvider(schemaName -> new SchemaMetadata());
    SchemaMetadata schemaMetadata = provider.getSchemaMetadata("foo");
    assertSame(provider, schemaMetadata.getSchemaMetadataProvider());
  }
}
