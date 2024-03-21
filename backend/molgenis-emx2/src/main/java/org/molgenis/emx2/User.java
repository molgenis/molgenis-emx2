package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class User extends HasSettings<User> {
  private static final String TOKENS = "access-tokens";
  private String username;
  private Database database;

  public User(String username) {
    // will result in exceptions if you use methods that need database
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
    if (database == null) {
      throw new MolgenisException(
          "Cannot call user.addToken  unless you instantiate using constructor including database parameter");
    }
    String tokensString = getSetting(TOKENS);
    if (tokensString == null) {
      tokensString = tokenId;
    } else {
      tokensString += "," + tokenId;
    }
    this.setSetting(TOKENS, tokensString);
    database.saveUser(this);
  }
}
