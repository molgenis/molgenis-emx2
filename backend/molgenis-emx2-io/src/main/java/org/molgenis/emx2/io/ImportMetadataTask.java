package org.molgenis.emx2.io;

import static org.molgenis.emx2.io.ImportSchemaTask.MOLGENIS_ONTOLOGIES;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.emx2.Emx2Members;
import org.molgenis.emx2.io.emx2.Emx2Settings;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Step;
import org.molgenis.emx2.tasks.Task;

public class ImportMetadataTask extends Task {
  public static final String MOLGENIS = "molgenis";
  private TableStore store;
  private Schema schema;

  public ImportMetadataTask(Schema schema, TableStore store, boolean strict) {
    super("Import metadata", strict);
    this.schema = schema;
    this.store = store;
  }

  @Override
  public void run() {
    this.start();
    try {
      if (store.containsTable("attributes")) {
        Emx1.uploadFromStoreToSchema(store, schema);
        this.complete("Imported emx1 metadata");
      }

      if (store.containsTable(MOLGENIS)) {
        schema.migrate(Emx2.fromRowList(store.readTable(MOLGENIS)));
        this.step("Loaded tables and columns from 'molgenis' sheet").complete();
      } else {
        this.step("Metadata loading skipped: 'molgenis' sheet not included in the file").skipped();
      }

      if (store.containsTable("molgenis_members")) {
        Step step = this.step("Loaded members from 'molgenis_members' sheet").start();
        int count = Emx2Members.inputRoles(store, schema);
        step.setTotal(count).complete();
      } else {
        this.step("Members loading skipped: 'molgenis_members' sheet not included in the file")
            .skipped();
      }

      if (store.containsTable("molgenis_settings")) {
        Step step = this.step("Loaded settings from 'molgenis_settings' sheet").start();
        Emx2Settings.inputSettings(store, schema);
        step.complete();
      } else {
        this.step("Loading settings skipped: 'molgenis_settings' sheet not included in the file")
            .skipped();
      }

      // import ontologies, if any
      if (store.containsTable("molgenis_ontologies")) {
        Step step = this.step("Loaded settings from 'molgenis_ontologies' sheet").start();
        int count = loadOntologiesFromMolgenisOntologiesSheet(schema, store);
        step.setTotal(count).complete();
      } else {
        this.step(
                "Loading molgenis_ontologies skipped: 'molgenis_ontologies' sheet not included in the file")
            .skipped();
      }
      this.complete();
    } catch (Exception e) {
      this.completeWithError(e.getMessage());
      throw e;
    }
  }

  private int loadOntologiesFromMolgenisOntologiesSheet(Schema schema, TableStore store) {
    Map<String, List<Row>> batches = new LinkedHashMap<>();
    int count = 0;
    for (Row r : store.readTable(MOLGENIS_ONTOLOGIES)) {
      String ontology = r.getString("ontology");
      if (!batches.containsKey(ontology)) {
        batches.put(ontology, new LinkedList<>());
      }
      r.getValueMap().remove("ontology");
      batches.get(ontology).add(r);
      count++;
    }
    for (Map.Entry<String, List<Row>> entry : batches.entrySet()) {
      Table table = schema.getTable(entry.getKey());
      if (table == null) {
        throw new MolgenisException(
            String.format(
                "Import molgenis_ontologies failed: ontology=%s is unknown.", entry.getKey()));
      }
      table.save(entry.getValue());
    }
    return count;
  }
}
