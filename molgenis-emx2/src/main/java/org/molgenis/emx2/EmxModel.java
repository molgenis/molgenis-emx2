package org.molgenis.emx2;

import java.util.Collection;

public interface EmxModel {
    Collection<String> getTableNames();

    EmxTable getTable(String name);

    Collection<EmxTable> getTables();
}
