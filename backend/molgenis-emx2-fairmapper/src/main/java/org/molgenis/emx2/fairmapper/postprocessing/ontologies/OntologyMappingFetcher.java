package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import java.util.Map;

public interface OntologyMappingFetcher {

  /**
   * @param schemaName name of schema that contains the ontology table to fetch the mapping of
   * @param tableName name of the ontology table to fetch the mapping of
   * @return Ontology mapping in the shape of a map where the key is the ontologyTermURI field
   *     and the value is it's corresponding name.
   */
  Map<String, String> getMapping(String schemaName, String tableName);
}
