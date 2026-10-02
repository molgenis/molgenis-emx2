package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;

/**
 * A post-processor as written in a pipeline file. The name it is referred to by in the file is set
 * with {@link com.fasterxml.jackson.annotation.JsonTypeName} on the implementing record, and every
 * implementation has to be listed in {@link JsonSubTypes} below.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({@JsonSubTypes.Type(CoalesceFieldSpec.class)})
public interface PostProcessorSpec {

  /**
   * Checks the arguments read from the pipeline file. Called after the whole entry has been read,
   * so unknown arguments are reported before missing ones.
   *
   * @throws org.molgenis.emx2.MolgenisException when an argument is missing or invalid
   */
  default void validate() {}

  PostProcessor create(PipelineContext context);
}
