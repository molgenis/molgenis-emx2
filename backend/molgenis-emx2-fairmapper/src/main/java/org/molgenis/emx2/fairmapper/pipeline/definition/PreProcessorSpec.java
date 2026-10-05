package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;

/**
 * A pre-processor as written in a pipeline file. The name it is referred to by in the file is set
 * with {@link com.fasterxml.jackson.annotation.JsonTypeName} on the implementing record, and every
 * implementation has to be listed in {@link JsonSubTypes} below.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
  @JsonSubTypes.Type(TemporalSpec.class),
  @JsonSubTypes.Type(TypicalAgeSpec.class),
  @JsonSubTypes.Type(StageCsvwSpec.class)
})
public interface PreProcessorSpec {

  RdfPreProcessor create(PipelineContext context);
}
