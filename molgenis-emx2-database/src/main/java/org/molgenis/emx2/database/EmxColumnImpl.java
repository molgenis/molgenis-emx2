package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxColumn;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;

public class EmxColumnImpl extends EmxColumn {
  public EmxColumnImpl(EmxTable table, String name, EmxType type) {
    super(table, name, type);
  }
}
