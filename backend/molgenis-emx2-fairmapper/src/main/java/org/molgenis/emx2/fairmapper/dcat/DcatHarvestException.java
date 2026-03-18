package org.molgenis.emx2.fairmapper.dcat;

import org.molgenis.emx2.MolgenisException;

public class DcatHarvestException extends MolgenisException {
  public DcatHarvestException(String message) {
    super(message);
  }

  public DcatHarvestException(String message, Throwable cause) {
    super(message, cause);
  }
}
