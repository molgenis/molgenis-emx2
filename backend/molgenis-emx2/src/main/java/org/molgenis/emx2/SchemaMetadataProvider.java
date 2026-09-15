package org.molgenis.emx2;

public interface SchemaMetadataProvider {

  SchemaMetadata getSchemaMetadata(String schemaName);

  DatabaseListener getListener();
}
