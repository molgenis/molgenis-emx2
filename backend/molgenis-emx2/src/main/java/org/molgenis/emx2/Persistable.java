package org.molgenis.emx2;

import org.molgenis.emx2.beans.Mapper;

public interface Persistable<T> {

  default Row toRow() {
    return Mapper.map(this)[0];
  }
}
