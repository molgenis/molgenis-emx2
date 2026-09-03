package org.molgenis.emx2.fairmapper.schemas;

import java.util.Optional;
import org.molgenis.emx2.SchemaMetadata;

public interface SchemaFetcher {

  Optional<SchemaMetadata> fetch(String schemaName);
}
