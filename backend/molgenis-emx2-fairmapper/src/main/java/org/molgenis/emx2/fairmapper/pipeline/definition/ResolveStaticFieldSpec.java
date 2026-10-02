package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveStaticFieldPostProcessor;

/**
 * Pipeline file entry for {@link ResolveStaticFieldPostProcessor}.
 *
 * <pre>
 * - resolve-static:
 *     table: Collections
 *     field: type
 *     value: http://semanticscience.org/resource/SIO_001067
 * </pre>
 */
@JsonTypeName("resolve-static")
public record ResolveStaticFieldSpec(String table, String field, Object value)
    implements PostProcessorSpec {

  @Override
  public void validate() {
    if (table == null || field == null || value == null) {
      throw new MolgenisException(
          "Invalid pipeline file: resolve-static requires table, field and value");
    }
  }

  @Override
  public PostProcessor create(PipelineContext context) {
    return new ResolveStaticFieldPostProcessor(table, field, value);
  }
}
