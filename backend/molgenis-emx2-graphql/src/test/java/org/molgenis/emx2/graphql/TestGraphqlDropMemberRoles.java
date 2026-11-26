package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.DATE;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlDropMemberRoles {

  private static final String SCHEMA_NAME = TestGraphqlDropMemberRoles.class.getSimpleName();
  private static final String PATIENT_VIEWER = "PatientViewer";
  private static final String SYSTEM_ROLE = Privileges.VIEWER.toString();
  private static final String DOCTOR_USER = "dropMemberRolesDoctor";

  private static Schema schema;
  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Hospital")
            .add(column("name").setPkey())
            .add(column("city"))
            .add(column("doctors").setType(REFBACK).setRefTable("Doctor").setRefBack("hospital")),
        table("Doctor")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("specialty"))
            .add(column("hospital").setType(REF).setRefTable("Hospital"))
            .add(column("patients").setType(REFBACK).setRefTable("Patient").setRefBack("doctor")),
        table("Patient")
            .add(column("id").setPkey())
            .add(column("name"))
            .add(column("birthDate").setType(DATE))
            .add(column("doctor").setType(REF).setRefTable("Doctor"))
            .add(column("hospital").setType(REF).setRefTable("Hospital")));
    schema.createRole(PATIENT_VIEWER);
    schema.grant(PATIENT_VIEWER, new TablePermission("Patient").select(true));
    graphql = new GraphqlExecutor(schema, new TaskServiceInMemory());
  }

  @BeforeEach
  void assignSystemAndCustomRole() {
    schema.addMember(new Username(DOCTOR_USER), SYSTEM_ROLE);
    schema.addMember(new Username(DOCTOR_USER), PATIENT_VIEWER);
    assertEquals(List.of(PATIENT_VIEWER, SYSTEM_ROLE), rolesOfDoctorUser());
  }

  @Test
  void shouldDropOnlyTheGivenRoleOfMember() throws IOException {
    execute(
        "mutation{drop(memberRoles:[{email:\""
            + DOCTOR_USER
            + "\",role:\""
            + PATIENT_VIEWER
            + "\"}]){message}}");

    assertEquals(List.of(SYSTEM_ROLE), rolesOfDoctorUser());
  }

  @Test
  void shouldStillDropAllRolesWhenDroppingMemberByEmail() throws IOException {
    execute("mutation{drop(members:[\"" + DOCTOR_USER + "\"]){message}}");

    assertEquals(List.of(), rolesOfDoctorUser());
  }

  @Test
  void shouldRequireRoleWhenDroppingMemberRole() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () ->
                execute("mutation{drop(memberRoles:[{email:\"" + DOCTOR_USER + "\"}]){message}}"));

    assertTrue(exception.getMessage().contains("both email and role are required"));
    assertEquals(List.of(PATIENT_VIEWER, SYSTEM_ROLE), rolesOfDoctorUser());
  }

  private static List<String> rolesOfDoctorUser() {
    return schema.getMembers().stream()
        .filter(member -> DOCTOR_USER.equals(member.getUsername().value()))
        .map(Member::getRole)
        .sorted()
        .toList();
  }

  private static JsonNode execute(String query) throws IOException {
    String result = convertExecutionResultToJson(graphql.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").get(0).get("message").asText());
    }
    return node.get("data");
  }
}
