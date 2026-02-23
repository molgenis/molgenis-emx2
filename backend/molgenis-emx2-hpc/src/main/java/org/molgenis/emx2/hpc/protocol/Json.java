package org.molgenis.emx2.hpc.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared Jackson ObjectMapper for the HPC module. */
public final class Json {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  private Json() {}
}
