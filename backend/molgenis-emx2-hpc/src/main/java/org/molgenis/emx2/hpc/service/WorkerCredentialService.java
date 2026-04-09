package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.hpc.HpcFields.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.hpc.HpcTables;
import org.molgenis.emx2.sql.SqlDatabase;

/** Per-worker credential issuance, rotation, revocation, and verification support. */
public class WorkerCredentialService {

  public static final String CREDENTIALS_KEY_SETTING = "MOLGENIS_HPC_CREDENTIALS_KEY";

  private static final String STATUS_ACTIVE = "ACTIVE";
  private static final String STATUS_REVOKED = "REVOKED";
  private static final String STATUS_EXPIRED = "EXPIRED";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int GCM_TAG_BITS = 128;
  private static final int GCM_IV_BYTES = 12;
  private static final int SECRET_BYTES = 32;

  private final TxHelper tx;
  private final String systemSchemaName;

  public record AuthenticatedCredential(String id, String secret) {}

  public record IssuedCredential(
      String id,
      String workerId,
      String secret,
      String status,
      String label,
      LocalDateTime createdAt,
      LocalDateTime expiresAt) {}

  public WorkerCredentialService(SqlDatabase database, String systemSchemaName) {
    this.tx = new TxHelper(database);
    this.systemSchemaName = systemSchemaName;
  }

  /** Issues a new credential. Fails if an active credential already exists for the worker. */
  public IssuedCredential issueCredential(
      String workerId, String label, LocalDateTime expiresAt, String createdBy) {
    requireWorkerId(workerId);
    String plaintextSecret = generateSecret();
    return tx.txResult(
        db -> {
          Table table = credentialsTable(db);
          LocalDateTime now = LocalDateTime.now();
          expireOverdueCredentials(table, workerId, now);
          if (hasActiveCredential(table, workerId)) {
            throw new IllegalStateException(
                "Worker " + workerId + " already has an active credential");
          }
          // Worker identities are provisioned at credential issue/rotate time.
          // This makes bootstrap deterministic even before first register().
          ensureWorkerIdentityExists(db, workerId);
          String keyMaterial = requireCredentialsKey(db);
          String id = UUID.randomUUID().toString();
          table.insert(
              row(
                  ID, id,
                  WORKER_ID, workerId,
                  SECRET_ENCRYPTED, encrypt(plaintextSecret, keyMaterial),
                  STATUS, STATUS_ACTIVE,
                  LABEL, label,
                  CREATED_AT, now,
                  CREATED_BY, createdBy,
                  EXPIRES_AT, expiresAt));
          return new IssuedCredential(
              id, workerId, plaintextSecret, STATUS_ACTIVE, label, now, expiresAt);
        });
  }

  /** Rotates the worker credential by revoking active credentials and issuing a replacement. */
  public IssuedCredential rotateCredential(
      String workerId, String label, LocalDateTime expiresAt, String createdBy) {
    requireWorkerId(workerId);
    String plaintextSecret = generateSecret();
    return tx.txResult(
        db -> {
          Table table = credentialsTable(db);
          LocalDateTime now = LocalDateTime.now();
          expireOverdueCredentials(table, workerId, now);
          revokeActiveCredentials(table, workerId, now);
          ensureWorkerIdentityExists(db, workerId);

          String keyMaterial = requireCredentialsKey(db);
          String id = UUID.randomUUID().toString();
          table.insert(
              row(
                  ID, id,
                  WORKER_ID, workerId,
                  SECRET_ENCRYPTED, encrypt(plaintextSecret, keyMaterial),
                  STATUS, STATUS_ACTIVE,
                  LABEL, label,
                  CREATED_AT, now,
                  CREATED_BY, createdBy,
                  EXPIRES_AT, expiresAt));
          return new IssuedCredential(
              id, workerId, plaintextSecret, STATUS_ACTIVE, label, now, expiresAt);
        });
  }

  /** Revokes a credential by id. Returns null when the credential does not exist. */
  public Row revokeCredential(String workerId, String credentialId) {
    requireWorkerId(workerId);
    if (credentialId == null || credentialId.isBlank()) {
      throw new IllegalArgumentException("credential id is required");
    }
    return tx.txResult(
        db -> {
          Table table = credentialsTable(db);
          List<Row> rows = table.where(f(ID, EQUALS, credentialId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row credential = rows.getFirst();
          if (!workerId.equals(credential.getString(WORKER_ID))) {
            return null;
          }
          String status = credential.getString(STATUS);
          if (STATUS_ACTIVE.equals(status)) {
            credential.set(STATUS, STATUS_REVOKED);
            credential.set(REVOKED_AT, LocalDateTime.now());
            table.update(credential);
          }
          return credential;
        });
  }

  /** Lists credential metadata rows for a worker ordered by created_at desc. */
  public List<Row> listCredentials(String workerId) {
    requireWorkerId(workerId);
    return tx.txResult(
        db -> {
          Table table = credentialsTable(db);
          LocalDateTime now = LocalDateTime.now();
          expireOverdueCredentials(table, workerId, now);
          List<Row> rows =
              new ArrayList<>(table.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows());
          rows.sort(
              Comparator.comparing(
                      (Row r) -> DateTimeUtil.parse(r.getString(CREATED_AT)),
                      Comparator.nullsLast(Comparator.reverseOrder()))
                  .thenComparing(r -> r.getString(ID), Comparator.nullsLast(String::compareTo)));
          return rows;
        });
  }

  /**
   * Resolves an active credential for worker-authenticated requests.
   *
   * <p>Throws SecurityException for unknown/revoked/expired worker credentials.
   */
  public AuthenticatedCredential resolveActiveCredential(String workerId) {
    requireWorkerId(workerId);
    try {
      AuthenticatedCredential credential =
          tx.txResult(db -> resolveCredentialInTransaction(db, workerId));
      if (credential == null) {
        throw new SecurityException("No active credential for worker " + workerId);
      }
      return credential;
    } catch (RuntimeException e) {
      throw unwrapKnownException(e);
    }
  }

  private AuthenticatedCredential resolveCredentialInTransaction(
      org.molgenis.emx2.Database db, String workerId) {
    Table table = credentialsTable(db);
    expireOverdueCredentials(table, workerId, LocalDateTime.now());

    List<Row> rows = table.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
    Row active = findMostRecentActive(rows);
    if (active == null) {
      return null;
    }

    String keyMaterial = requireCredentialsKey(db);
    String secret = decrypt(active.getString(SECRET_ENCRYPTED), keyMaterial);
    return new AuthenticatedCredential(active.getString(ID), secret);
  }

  private static Row findMostRecentActive(List<Row> rows) {
    Row active = null;
    for (Row row : rows) {
      if (!STATUS_ACTIVE.equals(row.getString(STATUS))) {
        continue;
      }
      if (active == null) {
        active = row;
        continue;
      }
      LocalDateTime activeTs = DateTimeUtil.parse(active.getString(CREATED_AT));
      LocalDateTime rowTs = DateTimeUtil.parse(row.getString(CREATED_AT));
      if (rowTs != null && (activeTs == null || rowTs.isAfter(activeTs))) {
        active = row;
      }
    }
    return active;
  }

  private static RuntimeException unwrapKnownException(RuntimeException e) {
    Throwable root = e;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    if (root instanceof SecurityException securityException) {
      throw securityException;
    }
    if (root instanceof IllegalStateException illegalStateException) {
      throw illegalStateException;
    }
    throw e;
  }

  /** Touches credential usage metadata on successful authentication. */
  public void markCredentialUsed(String credentialId) {
    if (credentialId == null || credentialId.isBlank()) {
      return;
    }
    tx.tx(
        db -> {
          Table table = credentialsTable(db);
          List<Row> rows = table.where(f(ID, EQUALS, credentialId)).retrieveRows();
          if (rows.isEmpty()) {
            return;
          }
          Row credential = rows.getFirst();
          credential.set(LAST_USED_AT, LocalDateTime.now());
          table.update(credential);
        });
  }

  private static String generateSecret() {
    byte[] bytes = new byte[SECRET_BYTES];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private Table credentialsTable(Database db) {
    Schema schema = db.getSchema(systemSchemaName);
    return schema.getTable(HpcTables.WORKER_CREDENTIALS);
  }

  private void ensureWorkerIdentityExists(Database db, String workerId) {
    Table workersTable = db.getSchema(systemSchemaName).getTable(HpcTables.WORKERS);
    List<Row> rows = workersTable.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
    if (rows.isEmpty()) {
      LocalDateTime now = LocalDateTime.now();
      // Populate the canonical registration fields so the identity row is complete
      // before first daemon register().
      workersTable.insert(
          row(
              WORKER_ID, workerId,
              HOSTNAME, workerId,
              REGISTERED_AT, now,
              LAST_HEARTBEAT_AT, now));
    }
  }

  private static void requireWorkerId(String workerId) {
    if (workerId == null || workerId.isBlank()) {
      throw new IllegalArgumentException("worker id is required");
    }
  }

  private static String requireCredentialsKey(Database db) {
    String key = db.getSetting(CREDENTIALS_KEY_SETTING);
    if (key == null || key.isBlank()) {
      throw new IllegalStateException(
          CREDENTIALS_KEY_SETTING + " must be set to use worker credentials");
    }
    return key;
  }

  private static boolean hasActiveCredential(Table table, String workerId) {
    List<Row> rows = table.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
    for (Row row : rows) {
      if (STATUS_ACTIVE.equals(row.getString(STATUS))) {
        return true;
      }
    }
    return false;
  }

  private static void revokeActiveCredentials(Table table, String workerId, LocalDateTime now) {
    List<Row> rows = table.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
    for (Row row : rows) {
      if (!STATUS_ACTIVE.equals(row.getString(STATUS))) {
        continue;
      }
      row.set(STATUS, STATUS_REVOKED);
      row.set(REVOKED_AT, now);
      table.update(row);
    }
  }

  private static void expireOverdueCredentials(Table table, String workerId, LocalDateTime now) {
    List<Row> rows = table.where(f(WORKER_ID, EQUALS, workerId)).retrieveRows();
    for (Row row : rows) {
      if (!STATUS_ACTIVE.equals(row.getString(STATUS))) {
        continue;
      }
      LocalDateTime expiresAt = DateTimeUtil.parse(row.getString(EXPIRES_AT));
      if (expiresAt != null && now.isAfter(expiresAt)) {
        row.set(STATUS, STATUS_EXPIRED);
        row.set(REVOKED_AT, now);
        table.update(row);
      }
    }
  }

  private static byte[] deriveAesKey(String keyMaterial) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to derive credential encryption key", e);
    }
  }

  private static String encrypt(String plaintext, String keyMaterial) {
    try {
      byte[] iv = new byte[GCM_IV_BYTES];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(deriveAesKey(keyMaterial), "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] payload = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
      return Base64.getEncoder().encodeToString(payload);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to encrypt worker credential", e);
    }
  }

  private static String decrypt(String encryptedBase64, String keyMaterial) {
    try {
      byte[] payload = Base64.getDecoder().decode(encryptedBase64);
      if (payload.length <= GCM_IV_BYTES) {
        throw new SecurityException("Credential payload is malformed");
      }
      byte[] iv = new byte[GCM_IV_BYTES];
      byte[] encrypted = new byte[payload.length - GCM_IV_BYTES];
      System.arraycopy(payload, 0, iv, 0, iv.length);
      System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);

      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(deriveAesKey(keyMaterial), "AES"),
          new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] plaintext = cipher.doFinal(encrypted);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (SecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new SecurityException("Credential decryption failed");
    }
  }

  /** Converts a credential row to API-safe metadata (without secret material). */
  public static Map<String, Object> toMetadata(Row row) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(ID, row.getString(ID));
    metadata.put(WORKER_ID, row.getString(WORKER_ID));
    metadata.put(STATUS, row.getString(STATUS));
    metadata.put(LABEL, row.getString(LABEL));
    metadata.put(CREATED_AT, row.getString(CREATED_AT));
    metadata.put(CREATED_BY, row.getString(CREATED_BY));
    metadata.put(LAST_USED_AT, row.getString(LAST_USED_AT));
    metadata.put(REVOKED_AT, row.getString(REVOKED_AT));
    metadata.put(EXPIRES_AT, row.getString(EXPIRES_AT));
    return metadata;
  }
}
