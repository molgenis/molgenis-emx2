package org.molgenis.emx2.fairmapper.rdf;

import java.io.IOException;
import org.eclipse.rdf4j.model.Model;

public interface RdfSource {
  Model fetch(String url) throws IOException;
}
