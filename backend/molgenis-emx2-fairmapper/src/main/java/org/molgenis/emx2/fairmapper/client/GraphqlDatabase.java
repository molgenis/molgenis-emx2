package org.molgenis.emx2.fairmapper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.schemas.GraphqlSchemaFetcher;
import org.molgenis.emx2.json.JsonUtil;

public class GraphqlDatabase implements Database {

  private static final JsonMapper MAPPER = new JsonMapper();

  private final GraphqlClient client;

  public GraphqlDatabase(GraphqlClient client) {
    this.client = client;
  }

  @Override
  public void tx(Transaction transaction) {
    transaction.run(this);
  }

  @Override
  public void init() {
    // no-op
  }

  @Override
  public Schema createSchema(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Schema createSchema(String name, String description) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Schema updateSchema(String name, String description) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Schema dropCreateSchema(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Schema dropCreateSchema(String name, String description) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void dropSchemaIfExists(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void dropSchema(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Collection<String> getSchemaNames() {
    String query = readFile("schema-names-query.graphql");
    JsonNode result = client.sendQuery(query);

    if (!result.has("_schemas")) {
      throw new MolgenisException("No schema returned in graphql response: " + result);
    }

    return result.get("_schemas").valueStream().map(node -> node.get("name").asText()).toList();
  }

  @Override
  public Collection<SchemaInfo> getSchemaInfos() {
    String query = readFile("schema-info-query.graphql");
    JsonNode result = client.sendQuery(query);

    if (!result.has("_schemas")) {
      throw new MolgenisException("No schema returned in graphql response: " + result);
    }

    try {
      return MAPPER.readerForListOf(SchemaInfo.class).readValue(result.get("_schemas"));
    } catch (IOException e) {
      throw new MolgenisException("Unable to map schema infos from graphql response", e);
    }
  }

  @Override
  public SchemaInfo getSchemaInfo(String schemaName) {
    return getSchemaInfos().stream()
        .filter(schemaInfo -> schemaInfo.tableSchema().equals(schemaName))
        .findFirst()
        .orElse(null);
  }

  @Override
  public Schema getSchema(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public SchemaMetadata getSchemaMetadata(String schemaName) {
    String query = readFile("schema-query.graphql");
    JsonNode result = client.sendSchemaQuery(schemaName, query);
    if (!result.has("_schema")) {
      throw new MolgenisException("No schema returned in graphql response: " + result);
    }

    try {
      SchemaMetadata schema = JsonUtil.jsonToSchema(result.get("_schema").toString());
      schema.setDatabase(this);
      schema.setName(schemaName);
      return schema;
    } catch (IOException e) {
      throw new MolgenisException("Unable to map query result to SchemaMetaData", e);
    }
  }

  @Override
  public List<Table> getTablesFromAllSchemas(String tableId) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public User addUser(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean checkUserPassword(String name, String password) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void setUserPassword(String name, String password) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean hasUser(String user) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public List<User> getUsers(int limit, int offset) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void removeUser(String name) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void setEnabledUser(String name, boolean enabled) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void setAdminUser(String name, boolean admin) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void setActiveUser(String username) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public String getActiveUser() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void clearActiveUser() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void grantCreateSchema(String user) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void setListener(DatabaseListener listener) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public DatabaseListener getListener() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean inTx() {
    return false;
  }

  @Override
  public void clearCache() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Integer getDatabaseVersion() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public int countUsers() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public String getAdminUserName() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean isAdmin() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void becomeAdmin() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean isOidcEnabled() {
    return false;
  }

  @Override
  public boolean hasSchema(String catalogueOntologies) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public User getUser(String userName) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void saveUser(User user) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public boolean isAnonymous() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database setBindings(Map<String, Supplier<Object>> bindings) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Map<String, Supplier<Object>> getJavaScriptBindings() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public List<LastUpdate> getLastUpdated() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public List<Member> loadUserRoles() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void revokeRoles(String userName, List<Map<String, String>> revokedRoles) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public void updateRoles(String userName, List<Map<String, String>> roles) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public String resolveJwtSharedSecret() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database clearSettings() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database setSettings(Map<String, String> settings) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database removeSetting(String key) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database setSetting(String key, String value) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public String getSetting(String key) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Map<String, String> getSettings() {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Database changeSettings(Map<String, String> settings) {
    throw new UnsupportedOperationException("Unsupported");
  }

  @Override
  public Optional<String> findSettingValue(String cssURL) {
    throw new UnsupportedOperationException("Unsupported");
  }

  private String readFile(String fileName) {
    try (InputStream inputStream = GraphqlSchemaFetcher.class.getResourceAsStream(fileName)) {
      return new String(Objects.requireNonNull(inputStream).readAllBytes());
    } catch (IOException e) {
      throw new MolgenisException("Unable to read query from file: " + fileName, e);
    }
  }
}
