package org.molgenis.emx2;

import java.util.List;

public interface EmxTable {

    String getName();

    EmxColumn getColumn(String name);

    List<EmxColumn> getColumns();

    List<EmxUnique> getUniques();

    EmxTable getExtend();

    EmxColumn getIdColumn();
}
