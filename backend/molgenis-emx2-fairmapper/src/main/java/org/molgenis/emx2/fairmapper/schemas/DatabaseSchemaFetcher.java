package org.molgenis.emx2.fairmapper.schemas;

import java.util.Optional;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;

public class DatabaseSchemaFetcher implements SchemaFetcher {

  private final Database database;

  public DatabaseSchemaFetcher(Database database) {
    this.database = database;
  }

  @Override
  public Optional<SchemaMetadata> fetch(String schemaName) {
    return Optional.ofNullable(database.getSchema(schemaName)).map(Schema::getMetadata);
  }
}
