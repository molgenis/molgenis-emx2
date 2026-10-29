package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface Database extends HasSettingsInterface<Database>, SchemaMetadataProvider {

  void tx(Transaction transaction);

  void init();

  Schema createSchema(String name);

  Schema createSchema(String name, String description);

  Schema updateSchema(String name, String description);

  Schema dropCreateSchema(String name);

  Schema dropCreateSchema(String name, String description);

  void dropSchemaIfExists(String name);

  void dropSchema(String name);

  Collection<String> getSchemaNames();

  Collection<SchemaInfo> getSchemaInfos();

  SchemaInfo getSchemaInfo(String schemaName);

  Schema getSchema(String name);

  default SchemaMetadata getSchemaMetadata(String schemaName) {
    Schema schema = getSchema(schemaName);
    if (schema == null) {
      throw new MolgenisException("Could not find schema metadata with name " + schemaName);
    }

    return schema.getMetadata();
  }

  List<Table> getTablesFromAllSchemas(String tableId);

  User addUser(Username name);

  boolean checkUserPassword(Username name, String password);

  void setUserPassword(Username name, String password);

  boolean hasUser(Username user);

  List<User> getUsers(int limit, int offset);

  void removeUser(Username name);

  void setEnabledUser(Username name, boolean enabled);

  void setAdminUser(Username name, boolean admin);

  void setActiveUser(Username username);

  Username getActiveUser();

  void clearActiveUser();

  void grantCreateSchema(Username user);

  void setListener(DatabaseListener listener);

  DatabaseListener getListener();

  boolean inTx();

  void clearCache();

  Integer getDatabaseVersion();

  int countUsers();

  /** for testing purposes */
  Username getAdminUserName();

  /**
   * check if the user that is logged in is admin
   *
   * <p>Shorthand for getAdminUserName().equals(getActiveUser()) to help understand code
   */
  boolean isAdmin();

  /**
   * change active user to admin
   *
   * <p>Shorthand for setActiveUser(getAdminUserName()) to help understand code
   */
  void becomeAdmin();

  boolean isOidcEnabled();

  boolean hasSchema(String catalogueOntologies);

  User getUser(Username userName);

  void saveUser(User user);

  boolean isAnonymous();

  Database setBindings(Map<String, Supplier<Object>> bindings);

  Map<String, Supplier<Object>> getJavaScriptBindings();

  List<LastUpdate> getLastUpdated();

  List<Member> loadUserRoles();

  void revokeRoles(Username userName, List<Map<String, String>> revokedRoles);

  void updateRoles(Username userName, List<Map<String, String>> roles);

  String resolveJwtSharedSecret();
}
