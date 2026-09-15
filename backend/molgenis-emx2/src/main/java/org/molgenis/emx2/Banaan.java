package org.molgenis.emx2;

public interface Banaan {

  SchemaMetadata getSchemaMetadata(String schemaName);

  DatabaseListener getListener();
}
