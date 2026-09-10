package org.molgenis.emx2;

import static java.util.Arrays.stream;

import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.rdf4j.model.IRI;

public interface HasSemantics {
  @Nullable
  Semantic[] getSemantics();

  SemanticPrefixes getSemanticPrefixes();

  /**
   * @return {@code false} if null or empty, otherwise {@code true}.
   */
  default boolean hasSemantics() {
    return getSemantics() != null && getSemantics().length > 0;
  }

  private <R> Stream<R> getSemanticsStream(Function<Semantic, R> mapper) {
    return getSemantics() == null ? Stream.empty() : stream(getSemantics()).map(mapper);
  }

  default Stream<IRI> getSemanticsIriStream() {
    return getSemanticsStream(getSemanticPrefixes()::mapAsIri);
  }

  /**
   * @see SemanticPrefixes#mapAsString(Semantic)
   */
  default Stream<String> getSemanticsStringStream() {
    return getSemanticsStream(getSemanticPrefixes()::mapAsString);
  }
}
