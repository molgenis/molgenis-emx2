package org.molgenis.emx2.cafevariome;

public record Response(int recordCount, Range recordRange, boolean exists) {}
