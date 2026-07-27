package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.molgenis.emx2.MolgenisException;

public interface RdfExtractor {

  default void addRdfToRepository(SailRepository repository, String location) {
    try {
      addRdfToRepository(repository, new URI(location));
    } catch (URISyntaxException e) {
      throw new MolgenisException("Invalid endpoint URL extracted from FDP: " + location, e);
    }
  }

  void addRdfToRepository(SailRepository repository, URI location);
}
