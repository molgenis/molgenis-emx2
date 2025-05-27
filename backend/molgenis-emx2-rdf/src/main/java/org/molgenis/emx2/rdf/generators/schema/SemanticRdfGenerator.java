package org.molgenis.emx2.rdf.generators.schema;

import org.apache.commons.lang3.NotImplementedException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.rdf.PrimaryKey;
import org.molgenis.emx2.rdf.writers.RdfWriter;

// todo: implement
public class SemanticRdfGenerator extends RdfGenerator implements RdfApiPaths {
  public SemanticRdfGenerator(RdfWriter writer, String baseURL) {
    super(writer, baseURL);
  }

  @Override
  public void generate(Schema schema) {
    throw new NotImplementedException();
  }

  @Override
  public void generate(Table table) {
    throw new NotImplementedException();
  }

  @Override
  public void generate(Table table, PrimaryKey primaryKey) {
    throw new NotImplementedException();
  }

  @Override
  public void generate(Table table, Column column) {
    throw new NotImplementedException();
  }
}
