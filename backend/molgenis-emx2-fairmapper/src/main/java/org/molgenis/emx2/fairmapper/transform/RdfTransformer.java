package org.molgenis.emx2.fairmapper.transform;

import java.util.List;
import org.eclipse.rdf4j.repository.Repository;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;

/**
 * Transformation step in the DCAT harvesting pipeline that converts an RDF repository into tabular
 * data.
 *
 * <p>Implementations query the repository and map the results onto a {@link TableStore}, bridging
 * the graph model to the row-column model expected by the rest of the EMX2 import pipeline. The
 * target {@code schema} and {@code tables} are passed in per call rather than fixed at construction
 * time, so a single transformer instance can be reused across different schemas.
 */
public interface RdfTransformer {

  /**
   * Transforms the RDF graph in {@code repository} into a {@link TableStore}.
   *
   * @param repository the RDF repository to query
   * @param schema metadata describing the target tables to map onto
   * @param tables names of the tables to populate
   * @return a {@link TableStore} containing the mapped tabular data
   */
  InMemoryTableStore transform(Repository repository, SchemaMetadata schema, List<String> tables);
}
