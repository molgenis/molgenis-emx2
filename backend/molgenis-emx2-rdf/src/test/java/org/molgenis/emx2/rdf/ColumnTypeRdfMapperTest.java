package org.molgenis.emx2.rdf;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;

class ColumnTypeRdfMapperTest {
    /**
     * If {@link ColumnTypeRdfMapper} misses any mappings for {@link ColumnType}, this test will fail.
     * Should prevent new types being added without implementing the API support as well.
     */
  @Test
  void validateAllColumnTypesCovered() {
    Set<ColumnType> columnTypes = Arrays.stream(ColumnType.values()).collect(Collectors.toSet());
    Set<ColumnType> columnMappings = ColumnTypeRdfMapper.getMapperKeys();

    Assertions.assertEquals(columnTypes, columnMappings);
  }
}
