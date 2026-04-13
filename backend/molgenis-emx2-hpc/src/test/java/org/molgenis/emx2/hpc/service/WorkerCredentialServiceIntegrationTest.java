package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class WorkerCredentialServiceIntegrationTest extends HpcServiceIntegrationTestBase {

  private static final String CREDENTIALS_KEY = "test-credentials-key-at-least-32-chars!";

  private WorkerCredentialService credentialService;

  @BeforeEach
  void setUpCredentialService() {
    // Reload settings from DB first — this SqlDatabase(false) instance has an incomplete
    // in-memory settings map, and setSetting persists the entire map. Without clearCache(),
    // it would overwrite the DB and wipe keys like MOLGENIS_JWT_SHARED_SECRET.
    database.clearCache();
    database.tx(
        db -> {
          db.becomeAdmin();
          db.setSetting(WorkerCredentialService.CREDENTIALS_KEY_SETTING, CREDENTIALS_KEY);
        });
    credentialService = new WorkerCredentialService(database, schemaName);
  }

  @Test
  void issueCredential_createsActiveCredentialAndReturnsSecret() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-1", "test-label", null, "admin");

    assertNotNull(issued.id());
    assertEquals("worker-1", issued.workerId());
    assertEquals("ACTIVE", issued.status());
    assertEquals("test-label", issued.label());
    assertNotNull(issued.secret());
    assertNotNull(issued.createdAt());
  }

  @Test
  void issueCredential_failsIfActiveCredentialAlreadyExists() {
    credentialService.issueCredential("worker-dup", null, null, "admin");

    assertThrows(
        RuntimeException.class,
        () -> credentialService.issueCredential("worker-dup", null, null, "admin"));
  }

  @Test
  void issueCredential_createsWorkerIdentityIfMissing() {
    credentialService.issueCredential("new-worker", null, null, null);

    // Worker identity should now exist — verify via workerService
    Row worker =
        database.getSchema(schemaName).getTable("HpcWorkers").query().retrieveRows().stream()
            .filter(r -> "new-worker".equals(r.getString("worker_id")))
            .findFirst()
            .orElse(null);
    assertNotNull(worker, "Worker identity should be auto-provisioned");
  }

  @Test
  void rotateCredential_revokesOldAndIssuesNew() {
    WorkerCredentialService.IssuedCredential first =
        credentialService.issueCredential("worker-rotate", "v1", null, "admin");

    WorkerCredentialService.IssuedCredential second =
        credentialService.rotateCredential("worker-rotate", "v2", null, "admin");

    assertNotEquals(first.id(), second.id());
    assertNotEquals(first.secret(), second.secret());
    assertEquals("ACTIVE", second.status());
    assertEquals("v2", second.label());

    // Old credential should be revoked
    List<Row> all = credentialService.listCredentials("worker-rotate");
    assertEquals(2, all.size());
    long activeCount = all.stream().filter(r -> "ACTIVE".equals(r.getString("status"))).count();
    assertEquals(1, activeCount, "Exactly one credential should be active after rotation");
  }

  @Test
  void revokeCredential_setsStatusToRevoked() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-revoke", null, null, "admin");

    Row revoked = credentialService.revokeCredential("worker-revoke", issued.id());
    assertNotNull(revoked);
    assertEquals("REVOKED", revoked.getString("status"));
    assertNotNull(revoked.getString("revoked_at"));
  }

  @Test
  void revokeCredential_returnsNullForNonExistentCredential() {
    assertNull(credentialService.revokeCredential("ghost-worker", "nonexistent-id"));
  }

  @Test
  void revokeCredential_returnsNullForWrongWorker() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-a", null, null, null);

    // Try to revoke with wrong worker ID
    assertNull(credentialService.revokeCredential("worker-b", issued.id()));
  }

  @Test
  void revokeCredential_rejectsBlankCredentialId() {
    assertThrows(
        IllegalArgumentException.class, () -> credentialService.revokeCredential("worker-x", ""));
  }

  @Test
  void listCredentials_returnsOrderedByCreatedAtDesc() {
    credentialService.issueCredential("worker-list", "first", null, null);
    credentialService.rotateCredential("worker-list", "second", null, null);

    List<Row> credentials = credentialService.listCredentials("worker-list");
    assertEquals(2, credentials.size());
    // Most recent first
    assertEquals("second", credentials.get(0).getString("label"));
    assertEquals("first", credentials.get(1).getString("label"));
  }

  @Test
  void listCredentials_rejectsBlankWorkerId() {
    assertThrows(IllegalArgumentException.class, () -> credentialService.listCredentials(""));
    assertThrows(IllegalArgumentException.class, () -> credentialService.listCredentials(null));
  }

  @Test
  void resolveActiveCredential_returnsSecretForActiveCredential() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-resolve", null, null, null);

    WorkerCredentialService.AuthenticatedCredential resolved =
        credentialService.resolveActiveCredential("worker-resolve");

    assertEquals(issued.id(), resolved.id());
    assertEquals(issued.secret(), resolved.secret());
  }

  @Test
  void resolveActiveCredential_throwsWhenNoActiveCredential() {
    // No credential issued for this worker
    workerService.registerOrHeartbeat("worker-no-cred", "host", List.of());

    assertThrows(
        SecurityException.class, () -> credentialService.resolveActiveCredential("worker-no-cred"));
  }

  @Test
  void resolveActiveCredential_throwsForRevokedCredential() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-revoked", null, null, null);
    credentialService.revokeCredential("worker-revoked", issued.id());

    assertThrows(
        SecurityException.class, () -> credentialService.resolveActiveCredential("worker-revoked"));
  }

  @Test
  void markCredentialUsed_updatesLastUsedAt() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-used", null, null, null);

    credentialService.markCredentialUsed(issued.id());

    List<Row> credentials = credentialService.listCredentials("worker-used");
    assertNotNull(credentials.get(0).getString("last_used_at"));
  }

  @Test
  void markCredentialUsed_ignoresBlankId() {
    assertDoesNotThrow(() -> credentialService.markCredentialUsed(null));
    assertDoesNotThrow(() -> credentialService.markCredentialUsed(""));
  }

  @Test
  void expiredCredential_isAutomaticallyExpired() {
    // Issue with an expiry in the past
    LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-expired", null, pastExpiry, null);

    // Listing triggers expiry check
    List<Row> credentials = credentialService.listCredentials("worker-expired");
    assertEquals(1, credentials.size());
    assertEquals("EXPIRED", credentials.get(0).getString("status"));

    // Should not be resolvable
    assertThrows(
        SecurityException.class, () -> credentialService.resolveActiveCredential("worker-expired"));
  }

  @Test
  void toMetadata_containsAllFieldsWithoutSecret() {
    WorkerCredentialService.IssuedCredential issued =
        credentialService.issueCredential("worker-meta", "my-label", null, "admin");

    List<Row> rows = credentialService.listCredentials("worker-meta");
    Map<String, Object> metadata = WorkerCredentialService.toMetadata(rows.get(0));

    assertEquals(issued.id(), metadata.get("id"));
    assertEquals("worker-meta", metadata.get("worker_id"));
    assertEquals("ACTIVE", metadata.get("status"));
    assertEquals("my-label", metadata.get("label"));
    assertNotNull(metadata.get("created_at"));
    assertFalse(metadata.containsKey("secret"), "Metadata must not contain secret");
    assertFalse(
        metadata.containsKey("secret_encrypted"), "Metadata must not contain encrypted secret");
  }
}
