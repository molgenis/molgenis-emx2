package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;

/**
 * Pipeline file entry for {@link TemporalRdfPreProcessor}. Takes no arguments.
 *
 * <pre>
 * - temporal
 * </pre>
 */
@JsonTypeName("temporal")
public record TemporalSpec() implements PreProcessorSpec {

  @Override
  public RdfPreProcessor create(PipelineContext context) {
    return new TemporalRdfPreProcessor();
  }
}
