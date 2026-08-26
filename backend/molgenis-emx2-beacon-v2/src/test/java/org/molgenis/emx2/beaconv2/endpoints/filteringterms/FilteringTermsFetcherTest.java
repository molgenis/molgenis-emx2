package org.molgenis.emx2.beaconv2.endpoints.filteringterms;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class FilteringTermsFetcherTest {

  private static final String SCHEMA = FilteringTermsFetcherTest.class.getSimpleName();
  private static final String PET = "Pet";

  private static final String USER_AGGREGATOR = "filtering_terms_aggregator";
  private static final String USER_EXISTS = "filtering_terms_exists";

  private static Database database;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    for (String user : List.of(USER_AGGREGATOR, USER_EXISTS)) {
      if (!database.hasUser(user)) database.addUser(user);
    }

    database.dropSchemaIfExists(SCHEMA);
    PET_STORE.getImportTask(database, SCHEMA, "", true).run();

    Schema schema = database.getSchema(SCHEMA);
    schema.removeMember(ANONYMOUS);
    // pet store puts row level security on Pet; that interaction is covered elsewhere
    schema.revoke("DragonKeeper", PET);
    schema.addMember(USER_AGGREGATOR, Privileges.AGGREGATOR.toString());
    schema.addMember(USER_EXISTS, Privileges.EXISTS.toString());
  }

  @AfterEach
  void becomeAdminAgain() {
    database.becomeAdmin();
  }

  @Test
  void adminGetsOntologyTerms() {
    assertFalse(ontologyLabels().isEmpty());
  }

  @Test
  void aggregatorGetsSameOntologyTermsAsAdmin() {
    Set<String> asAdmin = ontologyLabels();

    database.setActiveUser(USER_AGGREGATOR);
    assertEquals(
        asAdmin, ontologyLabels(), "aggregate roles must keep the filtering terms they had");
  }

  @Test
  void existsGetsAlphanumericTermsWithoutFailing() {
    database.setActiveUser(USER_EXISTS);
    Set<FilteringTerm> terms = assertDoesNotThrow(this::petFilteringTerms);

    assertTrue(terms.stream().anyMatch(term -> "alphanumeric".equals(term.getType())));
    assertTrue(
        terms.stream().noneMatch(term -> "ontology".equals(term.getType())),
        "grouping needs Range, so ontology terms are skipped rather than throwing");
  }

  private Set<String> ontologyLabels() {
    return petFilteringTerms().stream()
        .filter(term -> "ontology".equals(term.getType()))
        .map(FilteringTerm::getLabel)
        .collect(java.util.stream.Collectors.toSet());
  }

  private Set<FilteringTerm> petFilteringTerms() {
    Schema schema = database.getSchema(SCHEMA);
    return new FilteringTermsFetcher(database)
        .getFilteringTermsFromOneTable(SCHEMA, PET, schema.getTableNames());
  }
}
