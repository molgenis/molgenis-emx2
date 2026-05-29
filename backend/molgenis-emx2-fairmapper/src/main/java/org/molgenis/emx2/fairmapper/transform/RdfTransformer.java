package org.molgenis.emx2.fairmapper.transform;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.molgenis.emx2.io.tablestore.TableStore;

public interface RdfTransformer {

  TableStore transform(SailRepository repository);
}
