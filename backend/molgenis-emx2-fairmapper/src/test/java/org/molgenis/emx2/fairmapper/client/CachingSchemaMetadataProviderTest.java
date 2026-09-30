package org.molgenis.emx2.fairmapper.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;

class CachingSchemaMetadataProviderTest {

  @Test
  void shouldCacheSchemaMetadata() {
    CachingSchemaMetadataProvider cachingSchemaMetadataProvider =
        new CachingSchemaMetadataProvider(schemaName -> new SchemaMetadata());
    SchemaMetadata schemaMetadata = cachingSchemaMetadataProvider.getSchemaMetadata("foo");
    assertSame(schemaMetadata, cachingSchemaMetadataProvider.getSchemaMetadata("foo"));
    assertNotSame(schemaMetadata, cachingSchemaMetadataProvider.getSchemaMetadata("bar"));
  }
}
