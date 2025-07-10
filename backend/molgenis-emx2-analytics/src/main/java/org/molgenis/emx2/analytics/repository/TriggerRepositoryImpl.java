package org.molgenis.emx2.analytics.repository;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.molgenis.emx2.*;
import org.molgenis.emx2.analytics.model.Trigger;

public class TriggerRepositoryImpl implements TriggerRepository {
  static final String TRIGGER_TABLE_NAME = "AnalyticsTrigger";

  private static final String NAME = "name";
  private static final String CSS_SELECTOR = "cssSelector";
  private static final String APP_NAME = "appName";
  private static final String SCHEMA_NAME = "schemaName";

  private final Database database;

  public TriggerRepositoryImpl(Database database) {
    this.database = database;
    String currentUser = database.getActiveUser();
    this.database.runAsAdmin(
        db -> {
          Schema sysSchema = db.getSchema(SYSTEM_SCHEMA);
          if (sysSchema == null) {
            throw new MolgenisException("failed to setup Analytics TriggerRepository");
          }

          if (!sysSchema.getTableNames().contains(TRIGGER_TABLE_NAME)) {
            sysSchema.create(
                table(
                    TRIGGER_TABLE_NAME,
                    column(NAME).setPkey(),
                    column(CSS_SELECTOR).setType(ColumnType.TEXT).setRequired(true),
                    column(APP_NAME).setRequired(false),
                    column(SCHEMA_NAME).setRequired(false)));
          }
        });
  }

  @Override
  public Trigger addTrigger(Trigger trigger) {
    AtomicInteger inserted = new AtomicInteger();

    this.database.tx(
        db -> {
          Schema sysSchema = db.getSchema(SYSTEM_SCHEMA);
          if (sysSchema == null) {
            throw new MolgenisException("failed to add analytics Trigger");
          }
          Table triggerTable = sysSchema.getTable(TRIGGER_TABLE_NAME);
          Row triggerRow =
              row(
                  NAME,
                  trigger.name(),
                  CSS_SELECTOR,
                  trigger.cssSelector(),
                  APP_NAME,
                  trigger.appName(),
                  SCHEMA_NAME,
                  trigger.schemaName());
          inserted.set(triggerTable.insert(triggerRow));
        });

    if (inserted.get() == 1) {
      return trigger;
    } else {
      throw new MolgenisException("failed to add trigger");
    }
  }

  @Override
  public List<Trigger> getTriggersForSchema(Schema schema) {
    List<Trigger> triggers = new ArrayList<>();
    this.database.runAsAdmin(
        db -> {
          if (!schema.getInheritedRolesForActiveUser().contains(Privileges.VIEWER.toString())) {
            throw new MolgenisException(
                "failed fetch analytics triggers for schema " + schema.getName());
          }

          Schema sysSchema = db.getSchema(SYSTEM_SCHEMA);
          if (sysSchema == null) {
            throw new MolgenisException(
                "failed fetch analytics triggers for schema " + schema.getName());
          }

          Table triggerTable = sysSchema.getTable(TRIGGER_TABLE_NAME);
          List<Trigger> list =
              triggerTable
                  .select(
                      SelectColumn.s(NAME),
                      SelectColumn.s(CSS_SELECTOR),
                      SelectColumn.s(SCHEMA_NAME),
                      SelectColumn.s(APP_NAME))
                  .where(f(SCHEMA_NAME, EQUALS, schema.getName()))
                  .retrieveRows()
                  .stream()
                  .map(
                      r ->
                          new Trigger(
                              r.getString(NAME),
                              r.getText(CSS_SELECTOR),
                              r.getString(SCHEMA_NAME),
                              r.getString(APP_NAME)))
                  .toList();
          triggers.addAll(list);
        });
    return triggers;
  }

  @Override
  public boolean deleteTrigger(String triggerName) {
    AtomicInteger deleted = new AtomicInteger();

    this.database.tx(
        db -> {
          Schema sysSchema = db.getSchema(SYSTEM_SCHEMA);
          if (sysSchema == null) {
            throw new MolgenisException("failed to delete analytics Trigger");
          }

          Table triggerTable = sysSchema.getTable(TRIGGER_TABLE_NAME);
          List<Row> rowList =
              triggerTable.query().where(f(NAME, EQUALS, triggerName)).retrieveRows();
          if (1 != rowList.size()) {
            throw new MolgenisException("failed to delete trigger");
          }

          deleted.set(triggerTable.delete(rowList));
        });

    if (deleted.get() == 1) {
      return true;
    } else {
      throw new MolgenisException("failed to delete trigger");
    }
  }
  ;

  @Override
  public boolean updateTrigger(Trigger trigger) {
    AtomicInteger updated = new AtomicInteger();

    this.database.tx(
        db -> {
          Schema sysSchema = db.getSchema(SYSTEM_SCHEMA);
          if (sysSchema == null) {
            throw new MolgenisException("failed to update analytics Trigger");
          }

          Table triggerTable = sysSchema.getTable(TRIGGER_TABLE_NAME);
          List<Row> rowList =
              triggerTable.query().where(f(NAME, EQUALS, trigger.name())).retrieveRows();
          if (1 != rowList.size()) {
            throw new MolgenisException("failed to update trigger");
          }

          Row toUpdate = rowList.get(0);
          toUpdate.setText(CSS_SELECTOR, trigger.cssSelector());

          updated.set(triggerTable.update(rowList));
        });

    if (updated.get() == 1) {
      return true;
    } else {
      throw new MolgenisException("failed to update trigger");
    }
  }
}
