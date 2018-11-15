package org.molgenis.emx2;

import org.molgenis.emx2.beans.EmxTableBean;

import java.util.Collection;

public interface EmxModel {
    Collection<String> getTableNames();

    EmxTableBean getTable(String name);

    Collection<EmxTable> getTables();
}
