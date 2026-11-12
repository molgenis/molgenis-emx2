package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.rdf4j.repository.Repository;
import org.molgenis.emx2.MolgenisException;

public interface RdfExtractor {

  default void addRdfToRepository(Repository repository, String rootToAdd) {
    try {
      addRdfToRepository(repository, new URI(rootToAdd));
    } catch (URISyntaxException e) {
      throw new MolgenisException("Invalid endpoint URL extracted from FDP: " + rootToAdd, e);
    }
  }

  void addRdfToRepository(Repository repository, URI rootToAdd);
}
