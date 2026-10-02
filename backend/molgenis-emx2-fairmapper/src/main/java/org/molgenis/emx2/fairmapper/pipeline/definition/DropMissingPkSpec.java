package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.postprocessing.DropMissingPkRowPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;

/**
 * Pipeline file entry for {@link DropMissingPkRowPostProcessor}. The target schema comes from the
 * {@link PipelineContext}.
 *
 * <pre>
 * - drop-missing-pk:
 *     tables: [Organisations]
 * </pre>
 */
@JsonTypeName("drop-missing-pk")
public record DropMissingPkSpec(List<String> tables) implements PostProcessorSpec {

  @Override
  public void validate() {
    if (tables == null || tables.isEmpty()) {
      throw new MolgenisException("Invalid pipeline file: drop-missing-pk requires tables");
    }
  }

  @Override
  public PostProcessor create(PipelineContext context) {
    return new DropMissingPkRowPostProcessor(context.schema(), tables);
  }
}
