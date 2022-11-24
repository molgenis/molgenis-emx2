package org.molgenis.emx2.json;

import static org.molgenis.emx2.Constants.ANONYMOUS;

public record Setting(String key, String value, String user) {
  public Setting(String key, String value) {
    this(key, value, null);
  }

  public boolean isPublic() {
    return user == null || user.equals(ANONYMOUS);
  }
}
