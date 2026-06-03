package org.molgenis.emx2.fairmapper.transform;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.molgenis.emx2.io.tablestore.TableStore;

/**
 * Transformation step in the DCAT harvesting pipeline that converts an RDF repository into tabular
 * data.
 *
 * <p>Implementations query the repository and map the results onto a {@link TableStore}, bridging
 * the graph model to the row-column model expected by the rest of the EMX2 import pipeline.
 */
public interface RdfTransformer {

  /**
   * Transforms the RDF graph in {@code repository} into a {@link TableStore}.
   *
   * @param repository the RDF repository to query
   * @return a {@link TableStore} containing the mapped tabular data
   */
  TableStore transform(SailRepository repository);
}
