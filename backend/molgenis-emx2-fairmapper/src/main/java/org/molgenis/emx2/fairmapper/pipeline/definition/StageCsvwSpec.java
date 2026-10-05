package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.StageCsvwPreProcessor;

/**
 * Pipeline file entry for {@link StageCsvwPreProcessor}. Takes no arguments.
 *
 * <pre>
 * - stage-csvw
 * </pre>
 */
@JsonTypeName("stage-csvw")
public record StageCsvwSpec() implements PreProcessorSpec {

  @Override
  public RdfPreProcessor create(PipelineContext context) {
    return new StageCsvwPreProcessor();
  }
}
