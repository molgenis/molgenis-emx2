package org.molgenis.emx2;

import java.util.ArrayList;
import java.util.List;

/** we collect change handlers from 'afterSave' and 'afterDelete' and execute those on commit */
public abstract class TableListener {
  final List<Runnable> postCommitActions = new ArrayList<>();
  final String schemaName;
  final String tablenName;

  public TableListener(String schemaName, String tableName) {
    this.schemaName = schemaName;
    this.tablenName = tableName;
  }

  public final void preparePostSave(Iterable<Row> saved) {
    saved.forEach(save -> postCommitActions.add(afterSave(save)));
  }

  public final void preparePostDelete(Iterable<Row> deleted) {
    deleted.forEach(delete -> postCommitActions.add(afterDelete(delete)));
  }

  protected abstract Runnable afterSave(Row saved);

  protected abstract Runnable afterDelete(Row deleted);

  public void executePostCommit() {
    try {
      postCommitActions.forEach(Runnable::run);
    } finally {
      postCommitActions.clear();
    }
  }

  public String getSchemaName() {
    return schemaName;
  }

  public String getTableName() {
    return tablenName;
  }
}
