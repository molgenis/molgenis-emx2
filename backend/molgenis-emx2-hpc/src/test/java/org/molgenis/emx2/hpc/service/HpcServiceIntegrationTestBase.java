package org.molgenis.emx2.hpc.service;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.molgenis.emx2.sql.SqlDatabase;

abstract class HpcServiceIntegrationTestBase {

  protected SqlDatabase database;
  protected String schemaName;
  protected JobService jobService;
  protected ArtifactService artifactService;
  protected WorkerService workerService;

  @BeforeEach
  void setUpServices() {
    database = new SqlDatabase(false);
    database.setActiveUser(database.getAdminUserName());

    schemaName = "hpc_test_" + UUID.randomUUID().toString().replace("-", "");
    HpcSchemaInitializer.init(database, schemaName);

    workerService = new WorkerService(database, schemaName);
    jobService = new JobService(database, schemaName);
    artifactService = new ArtifactService(database, schemaName);
  }

  @AfterEach
  void tearDownSchema() {
    if (database != null && schemaName != null && database.getSchema(schemaName) != null) {
      database.dropSchema(schemaName);
    }
  }
}
