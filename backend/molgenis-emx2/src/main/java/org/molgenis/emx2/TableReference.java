package org.molgenis.emx2;

public record TableReference(String schemaName, String tableName) {
  public TableMetadata getTableMetadata(SchemaMetadata schemaMetadata) {
    if (schemaName != null && !schemaName.equals(schemaMetadata.getName())) {
      if (schemaMetadata.getDatabase() == null) {
        throw new MolgenisException(
            "Cannot retrieve table "
                + schemaName
                + "."
                + tableName
                + " from other schema without database");
      }
      schemaMetadata = schemaMetadata.getDatabase().getSchema(schemaName).getMetadata();
    }
    if (schemaMetadata == null) {
      throw new MolgenisException("Schema " + schemaName() + " not found");
    }
    TableMetadata tableMetadata = schemaMetadata.getTableMetadata(tableName());
    if (tableMetadata == null) {
      throw new MolgenisException("Table " + schemaName() + "." + tableName() + " not found");
    }
    return tableMetadata;
  }
}
