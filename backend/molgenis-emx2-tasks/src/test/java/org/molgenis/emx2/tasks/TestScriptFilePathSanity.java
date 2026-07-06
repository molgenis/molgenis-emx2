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
  private static Row extraFile(String filename) {
    return new Row(
        "extraFile",
        "attachment",
        "extraFile_filename",
        filename,
        "extraFile_extension",
        "csv",
        "extraFile_contents",
        new byte[] {1, 2, 3});
  }

  @Test
  void rejectsExtraFileResolvingOutsideTempDir() {
    ScriptTask task =
        new ScriptTask("extra file outside temp dir")
            .type(PYTHON)
            .extraFile(extraFile("../attachment.csv"))
            .script("print('never runs')");

    // setup fails before the script is executed, so run() reports the error and re-throws
    assertThrows(MolgenisException.class, task::run);
    assertEquals(ERROR, task.getStatus());
    assertTrue(task.getDescription().contains("illegal path"));
  }
}
