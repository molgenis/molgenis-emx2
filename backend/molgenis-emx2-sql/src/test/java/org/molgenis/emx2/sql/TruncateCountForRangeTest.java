package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.sql.SqlQuery.truncateCountForRange;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TruncateCountForRangeTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "0,  0",
    "1,  0",
    "9,  0",
    "10, 10",
    "11, 10",
    "17, 10",
    "99, 90",
    "100, 100",
    "101, 100",
    "112, 100",
    "999, 900",
    "1000, 1000",
    "1232, 1000",
    "9999, 9000",
    "10000, 10000",
    "54235, 50000"
  })
  void truncatesAsExpected(long input, long expected) {
    assertEquals(expected, truncateCountForRange(input));
  }
}
