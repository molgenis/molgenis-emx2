package org.molgenis.emx2.rdf.generators;

import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.TableColumnsSelector;

/** Contains a method for each possible API path that can be requested through the RDF API. */
public interface RdfApiGenerator {
  /** Generate RDF when calling the schema as a whole. */
  void generate(Schema schema, TableColumnsSelector selector);

  /** Generate RDF when calling a specific table. */
  void generate(Table table, TableColumnsSelector selector);

  /** Generate RDF when calling a specific row from a table. */
  void generate(Table table, TableColumnsSelector selector, PrimaryKey primaryKey);

  /** Generate RDF when calling a specific column from a table. */
  // todo: check implementation if selector should also be added
  void generate(Table table, Column column);
}
