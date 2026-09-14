package org.molgenis.emx2;

import java.util.Map;
import java.util.function.Supplier;

public interface Banaan {

  SchemaMetadata getSchemaMetadata(String schemaName);

  DatabaseListener getListener();

  Map<String, Supplier<Object>> getJavaScriptBindings(); // we would like to get rid of thiso ne
}
