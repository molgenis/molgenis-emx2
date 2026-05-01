package org.molgenis.emx2.hpc.service;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.SqlDatabase;

/**
 * Transaction helper that wraps the common pattern of running a lambda inside {@code database.tx()}
 * with {@code becomeAdmin()}, and optionally returning a value from the transaction.
 */
public final class TxHelper {

  private final SqlDatabase database;

  public TxHelper(SqlDatabase database) {
    this.database = database;
  }

  /**
   * Runs a function inside a transaction (with admin privileges) and returns its result.
   *
   * <p>Replaces the {@code T[] result = new T[1]; database.tx(db -> { ... result[0] = ...; });
   * return result[0];} pattern.
   */
  public <T> T txResult(Function<Database, T> fn) {
    AtomicReference<T> ref = new AtomicReference<>();
    database.tx(
        db -> {
          db.becomeAdmin();
          ref.set(fn.apply(db));
        });
    return ref.get();
  }

  /** Runs a consumer inside a transaction (with admin privileges). */
  public void tx(Consumer<Database> fn) {
    database.tx(
        db -> {
          db.becomeAdmin();
          fn.accept(db);
        });
  }
}
