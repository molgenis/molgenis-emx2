package org.molgenis.emx2.io;

import org.molgenis.emx2.Database;

public record SchemaLoaderSettings(
    Database database, String schemaName, String description, Boolean includeDemoData) {}
