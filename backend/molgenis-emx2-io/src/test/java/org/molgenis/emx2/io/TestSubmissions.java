package org.molgenis.emx2.io;

import static graphql.Assert.assertTrue;
import static junit.framework.TestCase.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.io.submission.SubmissionRecord.SubmissionStatus.*;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.DataCatalogueLoader;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.submission.SubmissionCreateTask;
import org.molgenis.emx2.io.submission.SubmissionService;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskService;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

public class TestSubmissions {
  private static Database db;
  private static SubmissionService service;
  private static TaskService taskService;

  @BeforeClass
  public static void setup() {
    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();
    // drop system schema if exists
    db.dropSchemaIfExists("system");
    // remove submissions
    db.getSchemaNames().stream()
        .filter(name -> name.startsWith("Submit_"))
        .forEach(
            name -> {
              db.dropSchemaIfExists(name);
            });
    taskService = new TaskServiceInMemory();
  }

  @Test
  public void createSubmissionSimple() {
    String targetSchemaName = TestSubmissions.class.getSimpleName() + "_1";
    // setup databases
    Schema targetSchema = db.dropCreateSchema(targetSchemaName);
    service = new SubmissionService(targetSchema, taskService);

    new PetStoreLoader().load(targetSchema, true);

    // config to clone 'pet' into submission schema
    SubmissionCreateTask createTask = service.createSubmission(List.of("Pet"), null);
    createTask.run();

    // verify a new schema has been created ready to receive submissions, with only table Pet
    Schema draft = db.getSchema(createTask.getSubmissionRecord().getSchema());
    assertNotNull(draft);
    assertEquals(1, draft.getTableNames().size());
    assertTrue(draft.getTableNames().contains("Pet"));
    assertTrue(
        draft
            .getTable("Pet")
            .getMetadata()
            .getColumn("category")
            .getRefSchema()
            .equals(targetSchemaName));
    // refback should be omitted
    assertNull(draft.getTable("Pet").getMetadata().getColumn("orders"));
    assertEquals(1, service.list().size());

    // check with a refback included
    createTask = service.createSubmission(List.of("Pet", "Order"), null);
    createTask.run();

    draft = db.getSchema(createTask.getSubmissionRecord().getSchema());
    assertEquals(2, draft.getTableNames().size());
    assertTrue(draft.getTableNames().contains("Pet"));
    assertTrue(draft.getTableNames().contains("Order"));
    // should now include the refback
    assertNotNull(draft.getTable("Pet").getMetadata().getColumn("orders"));

    // now add some contents and then merge!
    Table petTable = draft.getTable("Pet");
    petTable.save(row("name", "puck", "category", "cat", "weight", 20.0));
    Table orderTable = draft.getTable("Order");
    orderTable.save(
        row("orderId", 3, "pet", "puck", "quantity", 1, "price", 3.99, "status", "approved"));

    // lets merge
    service.mergeSubmission(createTask.getSubmissionRecord().getId()).run();
    assertEquals("puck", targetSchema.getTable("Pet").retrieveRows().get(8).getString("name"));
    assertEquals(MERGED, service.getById(createTask.getSubmissionRecord().getId()).getStatus());
  }

  @Test
  public void createSubmissionWithInheritance() {
    String targetSchemaName = TestSubmissions.class.getSimpleName() + "_2";

    // setup databases
    Schema targetSchema = db.dropCreateSchema(targetSchemaName);
    service = new SubmissionService(targetSchema, taskService);
    new DataCatalogueLoader().load(targetSchema, false);

    // config to clone 'cohort' into submission schema
    SubmissionCreateTask task =
        service.createSubmission(List.of("Cohorts"), "{pid: \"TestCohort\"}");
    task.run();

    // todo rest of tests
  }
}
