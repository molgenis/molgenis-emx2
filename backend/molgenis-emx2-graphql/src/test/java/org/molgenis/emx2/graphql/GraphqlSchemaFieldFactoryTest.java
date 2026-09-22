package org.molgenis.emx2.graphql;

import static java.util.function.Predicate.*;
import static org.junit.jupiter.api.Assertions.*;

import graphql.schema.GraphQLFieldDefinition;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class GraphqlSchemaFieldFactoryTest {

  private static final List<Privileges> AUTHORIZED_PRIVILEGES =
      List.of(Privileges.OWNER, Privileges.MANAGER);

  private Schema schema;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(GraphqlSchemaFieldFactoryTest.class.getSimpleName());
  }

  @Test
  void whenAuthorizedForMembers_thenIncludeInSchema() {
    for (Privileges privilege : AUTHORIZED_PRIVILEGES) {
      schema.getDatabase().becomeAdmin();
      schema.removeMember("test-user");
      schema.addMember("test-user", privilege.toString());
      schema.getDatabase().setActiveUser("test-user");

      GraphQLFieldDefinition.Builder builder = new GraphqlSchemaFieldFactory().schemaQuery(schema);
      GraphQLFieldDefinition definition = builder.build();
      assertTrue(
          definitionContainsMembersField(definition),
          "Authorization with privilege " + privilege + " failed");
    }
  }

  @Test
  void whenNotAuthorizedForMembers_thenExcludeFromSchema() {
    for (Privileges privilege : unauthorizedPrivileges()) {
      schema.getDatabase().becomeAdmin();
      schema.removeMember("test-user");
      schema.addMember("test-user", privilege.toString());
      schema.getDatabase().setActiveUser("test-user");

      GraphQLFieldDefinition.Builder builder = new GraphqlSchemaFieldFactory().schemaQuery(schema);
      GraphQLFieldDefinition definition = builder.build();
      assertFalse(
          definitionContainsMembersField(definition),
          "Authorization with privilege " + privilege + " failed");
    }
  }

  private List<Privileges> unauthorizedPrivileges() {
    return Arrays.stream(Privileges.values()).filter(not(AUTHORIZED_PRIVILEGES::contains)).toList();
  }

  private boolean definitionContainsMembersField(GraphQLFieldDefinition definition) {
    return definition.getType().getChildren().stream()
        .filter(GraphQLFieldDefinition.class::isInstance)
        .map(GraphQLFieldDefinition.class::cast)
        .map(GraphQLFieldDefinition::getName)
        .anyMatch(name -> name.equals(GraphqlConstants.MEMBERS));
  }
}
