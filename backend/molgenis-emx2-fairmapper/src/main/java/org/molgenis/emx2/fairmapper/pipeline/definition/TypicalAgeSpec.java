package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;

/**
 * Pipeline file entry for {@link TypicalAgeRdfPreProcessor}. Takes no arguments.
 *
 * <pre>
 * - typical-age
 * </pre>
 */
@JsonTypeName("typical-age")
public record TypicalAgeSpec() implements PreProcessorSpec {

  @Override
  public RdfPreProcessor create(PipelineContext context) {
    return new TypicalAgeRdfPreProcessor();
  }
}
