package org.molgenis.emx2.rdf.generators;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;

/** */
public interface RdfApiPaths {
  /** Generate RDF when calling the schema as a whole. */
  void generate(Schema schema);

  /** Generate RDF when calling a specific table. */
  void generate(Table table);

  /** Generate RDF when calling a specific row from a table. */
  void generate(Table table, PrimaryKey primaryKey);

  /** Generate RDF when calling a specific column from a table. */
  void generate(Table table, Column column);
}
