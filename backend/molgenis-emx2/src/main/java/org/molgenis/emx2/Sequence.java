package org.molgenis.emx2;

public interface Sequence {

  long getLimit();

  long getCurrentValue();

  long getNextValue();
}
