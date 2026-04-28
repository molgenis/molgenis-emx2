package org.molgenis.emx2.rdf.generators.query.mappers;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

public interface ColumnMapper {

  List<Projectable> getSelectors();

  List<GraphPattern> getPattern();
}
