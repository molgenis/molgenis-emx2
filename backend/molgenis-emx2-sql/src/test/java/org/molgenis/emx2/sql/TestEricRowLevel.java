package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.molgenis.emx2.Constants.MG_ROLES;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.*;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestEricRowLevel {

  private static Database database;
  private static Schema schema;

  private static final String SCHEMA = "ERIC";
  private static final String BIOBANKS = "Biobanks";
  private static final String COLLECTIONS = "Collections";
  private static final int EXPECTED_COUNTRY_COUNT = 35;
  private static final String TEST_COUNTRY = "DE";
  private static final String TEST_USER = "eric_rls_test_user_de";

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    schema = database.getSchema(SCHEMA);
    assumeTrue(schema != null, "ERIC schema not present in database — skipping TestEricRowLevel");
    if (!database.hasUser(TEST_USER)) database.addUser(TEST_USER);
  }

  @Test
  @Order(1)
  void countryRolesAreCreatedForEachCountry() {
    List<Row> biobanks = schema.getTable(BIOBANKS).retrieveRows();
    Set<String> countries =
        biobanks.stream().map(r -> r.getString("country")).collect(Collectors.toSet());

    assertEquals(
        EXPECTED_COUNTRY_COUNT,
        countries.size(),
        "Expected " + EXPECTED_COUNTRY_COUNT + " distinct countries in Biobanks");

    for (String country : countries) {
      if (!schema.getRoles().contains(country)) {
        schema.createRole(country);
      }
      schema.grant(
          country,
          new TablePermission(BIOBANKS)
              .select(true)
              .insert(true)
              .update(true)
              .delete(true)
              .rowLevel(true));
      schema.grant(
          country,
          new TablePermission(COLLECTIONS)
              .select(true)
              .insert(true)
              .update(true)
              .delete(true)
              .rowLevel(true));
    }

    List<String> roles = schema.getRoles();
    for (String country : countries) {
      assertTrue(roles.contains(country), "Role should exist for country: " + country);
    }
    Role deRole = schema.getRoleInfo(TEST_COUNTRY);
    assertTrue(
        deRole.permissions().stream()
            .filter(p -> BIOBANKS.equals(p.table()))
            .anyMatch(p -> Boolean.TRUE.equals(p.isRowLevel())),
        TEST_COUNTRY + " grant on Biobanks should be row-level");
  }

  @Test
  @Order(2)
  void biobanksAreAssignedToCountryRoles() {
    long start = System.nanoTime();

    List<Row> biobanks = schema.getTable(BIOBANKS).retrieveRows();
    List<Row> updated =
        biobanks.stream()
            .filter(r -> r.getString("country") != null)
            .map(r -> r.set(MG_ROLES, new String[] {r.getString("country")}))
            .toList();
    schema.getTable(BIOBANKS).update(updated);

    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.printf("Assigned mg_roles for %d biobanks in %d ms%n", updated.size(), ms);

    long unassigned =
        schema.getTable(BIOBANKS).retrieveRows().stream()
            .filter(r -> r.getString("country") != null)
            .filter(
                r -> r.getStringArray(MG_ROLES) == null || r.getStringArray(MG_ROLES).length == 0)
            .count();
    assertEquals(0, unassigned, "All biobanks with a country should have mg_roles assigned");
  }

  @Test
  @Order(3)
  void collectionsAreAssignedToCountryRoles() {
    long start = System.nanoTime();

    List<Row> collections = schema.getTable(COLLECTIONS).retrieveRows();
    List<Row> updated =
        collections.stream()
            .filter(r -> r.getString("country") != null)
            .map(r -> r.set(MG_ROLES, new String[] {r.getString("country")}))
            .toList();
    schema.getTable(COLLECTIONS).update(updated);

    long ms = (System.nanoTime() - start) / 1_000_000;
    System.out.printf("Assigned mg_roles for %d collections in %d ms%n", updated.size(), ms);

    long unassigned =
        schema.getTable(COLLECTIONS).retrieveRows().stream()
            .filter(r -> r.getString("country") != null)
            .filter(
                r -> r.getStringArray(MG_ROLES) == null || r.getStringArray(MG_ROLES).length == 0)
            .count();
    assertEquals(0, unassigned, "All collections with a country should have mg_roles assigned");
  }

  @Test
  @Order(4)
  void countryUserSeesOnlyOwnBiobanks() {
    schema.addMember(TEST_USER, TEST_COUNTRY);

    database.setActiveUser(TEST_USER);
    database.tx(
        db -> {
          long start = System.nanoTime();
          List<Row> rows = db.getSchema(SCHEMA).getTable(BIOBANKS).retrieveRows();
          long ms = (System.nanoTime() - start) / 1_000_000;

          System.out.printf(
              "User '%s' retrieved %d biobanks in %d ms%n", TEST_COUNTRY, rows.size(), ms);

          assertFalse(rows.isEmpty(), TEST_COUNTRY + " user should see at least one biobank");
          assertTrue(
              rows.stream().allMatch(r -> TEST_COUNTRY.equals(r.getString("country"))),
              TEST_COUNTRY + " user should only see biobanks for their country");
        });
    database.becomeAdmin();
  }

  @Test
  @Order(5)
  void countryUserSeesOnlyOwnCollections() {
    database.setActiveUser(TEST_USER);
    database.tx(
        db -> {
          long start = System.nanoTime();
          List<Row> rows = db.getSchema(SCHEMA).getTable(COLLECTIONS).retrieveRows();
          long ms = (System.nanoTime() - start) / 1_000_000;

          System.out.printf(
              "User '%s' retrieved %d collections in %d ms%n", TEST_COUNTRY, rows.size(), ms);

          assertFalse(rows.isEmpty(), TEST_COUNTRY + " user should see at least one collection");
          assertTrue(
              rows.stream().allMatch(r -> TEST_COUNTRY.equals(r.getString("country"))),
              TEST_COUNTRY + " user should only see collections for their country");
        });
    database.becomeAdmin();
  }

  @Test
  @Order(6)
  void countryUserCannotMutateOtherCountryBiobank() {
    List<Row> allBiobanks = schema.getTable(BIOBANKS).retrieveRows();
    Row otherBiobank =
        allBiobanks.stream()
            .filter(r -> !TEST_COUNTRY.equals(r.getString("country")))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No non-" + TEST_COUNTRY + " biobank with pid found"));

    otherBiobank.setString("description", "HACKED_BY_" + TEST_COUNTRY + "_USER");

    database.setActiveUser(TEST_USER);
    assertThrows(
        MolgenisException.class,
        () -> database.getSchema(SCHEMA).getTable(BIOBANKS).update(otherBiobank));
    database.becomeAdmin();
  }
}
