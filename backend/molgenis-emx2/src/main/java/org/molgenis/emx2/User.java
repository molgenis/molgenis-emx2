package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.*;

public class User extends HasSettings<User> {
  private static final String TOKENS = "access-tokens";
  private String username;
  private boolean enabled = true;
  private Database database;
  private boolean isAdmin = false;

  User(String username) {
    // for testing protected
    requireNonNull(username);
    this.username = username;
  }

  public User(Database database, String username) {
    this(username);
    requireNonNull(username);
    this.database = database;
  }

  public User(Database database, String username, Map<String, String> settings) {
    this(username);
    requireNonNull(username);
    this.database = database;
    super.setSettingsWithoutReload(settings);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    assert username != null;
    this.username = username;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(username, user.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username);
  }

  public boolean hasToken(String tokenId) {
    String tokensString = getSetting(TOKENS);
    if (tokensString != null) {
      List<String> tokens = Arrays.asList(tokensString.split(","));
      if (tokens.contains(tokenId)) {
        return true;
      }
    }
    return false;
  }

  public void addToken(String tokenId) {
    String tokensString = getSetting(TOKENS);
    if (tokensString == null) {
      tokensString = tokenId;
    } else {
      tokensString += "," + tokenId;
    }
    this.setSetting(TOKENS, tokensString);
    database.saveUser(this);
  }

  public void setAdmin(boolean admin) {
    this.isAdmin = admin;
  }

  public boolean isAdmin() {
    return isAdmin;
  }
}
