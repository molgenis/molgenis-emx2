package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.postprocessing.CoalesceFieldPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;

/**
 * Pipeline file entry for {@link CoalesceFieldPostProcessor}.
 *
 * <pre>
 * - coalesce-field:
 *     table: Collections
 *     field: id
 *     derive-from: [acronym, name]
 *     strict: false   # optional, defaults to true
 * </pre>
 */
@JsonTypeName("coalesce-field")
public record CoalesceFieldSpec(String table, String field, List<String> deriveFrom, Boolean strict)
    implements PostProcessorSpec {

  public CoalesceFieldSpec {
    if (strict == null) {
      strict = true;
    }
  }

  @Override
  public void validate() {
    if (table == null || field == null || deriveFrom == null || deriveFrom.isEmpty()) {
      throw new MolgenisException(
          "Invalid pipeline file: coalesce-field requires table, field and derive-from");
    }
  }

  @Override
  public PostProcessor create(PipelineContext context) {
    return new CoalesceFieldPostProcessor(table, field, strict, deriveFrom.toArray(String[]::new));
  }
}
