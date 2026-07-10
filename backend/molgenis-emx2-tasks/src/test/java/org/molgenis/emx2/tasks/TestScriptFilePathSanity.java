package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.tasks.ScriptType.PYTHON;
import static org.molgenis.emx2.tasks.TaskStatus.ERROR;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

class TestScriptFilePathSanity {
  @Test
  void rejectsExtraFileResolvingOutsideTempDir() {
    Row scriptMetadata = new Row(
        "extraFile",
        "attachment",
        "extraFile_filename",
        "../attachment.csv",
        "extraFile_extension",
        "csv",
        "extraFile_contents",
        new byte[] {1, 2, 3});

    ScriptTask task =
        new ScriptTask("extra file outside temp dir")
            .type(PYTHON)
            .extraFile(scriptMetadata)
            .script("print('never runs')");

    // setup fails before the script is executed, so run() reports the error and re-throws
    assertThrows(MolgenisException.class, task::run);
    assertEquals(ERROR, task.getStatus());
    assertTrue(task.getDescription().contains("illegal path"));
  }
}
