package org.molgenis.emx2.io.emx2;

import org.molgenis.emx2.SchemaMetadata;

public record Emx2YamlBundle(SchemaMetadata schema, int formatVersion, String version) {}
