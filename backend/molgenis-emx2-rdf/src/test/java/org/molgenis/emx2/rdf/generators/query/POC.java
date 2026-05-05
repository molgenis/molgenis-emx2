package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class POC {

  private SchemaMetadata schema;

  @Test
  void testCatalogueQuery() {
    schema = HarvestingTestSchema.create();
    schema = TestDatabaseFactory.getTestDatabase().getSchema("harvesting").getMetadata();
    TableQueryGenerator generator = new TableQueryGenerator();
    SelectQuery query = generator.generate(schema.getTableMetadata("Resources"));
    System.out.println(query.getQueryString());
  }
}
