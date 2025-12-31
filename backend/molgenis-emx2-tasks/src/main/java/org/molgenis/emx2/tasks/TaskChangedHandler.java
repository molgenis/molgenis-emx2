package org.molgenis.emx2.tasks;

import java.io.File;

public interface TaskChangedHandler {

  void handleChange(Task task);

  void handleOutputFile(Task task, File outFile);
}
