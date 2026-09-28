package org.molgenis.emx2;

public interface SchemaMetadataProvider {

  SchemaMetadata getSchemaMetadata(String schemaName);

  default DatabaseListener getListener() {
    return new DatabaseListener() {
      @Override
      public void onUserChange() {
        // no-op
      }
    };
  }
  ;
}
