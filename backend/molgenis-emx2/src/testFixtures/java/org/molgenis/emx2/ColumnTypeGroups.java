package org.molgenis.emx2;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ColumnTypeGroups {
  public static Set<ColumnType> EXCLUDE_REFERENCE_HEADING;
  public static Set<ColumnType> EXCLUDE_FILE_REFERENCE_HEADING;
  public static Set<ColumnType> EXCLUDE_FILE_PERIOD_REFERENCE_HEADING;
  public static Set<ColumnType> EXCLUDE_ARRAY_FILE_REFERENCE_HEADING;

  static {
    EXCLUDE_REFERENCE_HEADING =
        Arrays.stream(ColumnType.values())
            .filter(i -> !i.isReference())
            .filter(i -> !i.isHeading())
            .collect(Collectors.toUnmodifiableSet());

    EXCLUDE_FILE_REFERENCE_HEADING =
        EXCLUDE_REFERENCE_HEADING.stream()
            .filter(i -> !i.isFile())
            .collect(Collectors.toUnmodifiableSet());

    EXCLUDE_FILE_PERIOD_REFERENCE_HEADING =
        EXCLUDE_FILE_REFERENCE_HEADING.stream()
            .filter(
                i -> !Set.of(ColumnType.PERIOD, ColumnType.PERIOD_ARRAY).contains(i.getBaseType()))
            .collect(Collectors.toUnmodifiableSet());

    EXCLUDE_ARRAY_FILE_REFERENCE_HEADING =
        Arrays.stream(ColumnType.values())
            .filter(i -> !i.isArray())
            .filter(i -> !i.isFile())
            .filter(i -> !i.isReference())
            .filter(i -> !i.isHeading())
            .collect(Collectors.toUnmodifiableSet());
  }
}
