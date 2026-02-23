package org.molgenis.emx2;

public interface Sequence {

  String getName();

  long getLimit();

  long getCurrentValue();

  long getNextValue();
}
