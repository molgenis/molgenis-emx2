package org.molgenis.emx2.beaconv2;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import org.molgenis.emx2.MolgenisException;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum BeaconSpec {
  BEACON_V2("beacon"),
  BEACON_VP("beacon_vp");

  private final String path;

  public static BeaconSpec findByPath(String pathOther) {
    return Arrays.stream(values())
        .filter(beaconSpec -> beaconSpec.getPath().equalsIgnoreCase(pathOther))
        .findFirst()
        .orElseThrow(
            () -> new MolgenisException("Invalid beacon specification type: " + pathOther));
  }

  BeaconSpec(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}
