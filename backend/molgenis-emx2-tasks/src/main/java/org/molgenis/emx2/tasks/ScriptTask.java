package org.molgenis.emx2.tasks;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptTask extends Task {
  private static Logger logger = LoggerFactory.getLogger(ScriptTask.class);
  private String script;
  private String parameters;

  public ScriptTask(String name, String script, String parameters) {
    super("Executing script: " + name);
    // in the future we might want to create a commandGenerator plugin to specify the command for
    // other languages
    this.script = script;
    this.parameters = parameters;
  }

  @Override
  public void run() {
    this.start();
    // temporary files with script and to collect output
    Path tempScriptFile = null;
    try {
      // paste the script to a file
      tempScriptFile = Files.createTempFile("python", ".py");
      String inputJson = parameters != null ? parameters : "{}";
      Files.write(tempScriptFile, this.script.getBytes(UTF_8), StandardOpenOption.WRITE);
      String tempScriptFilePath = tempScriptFile.toAbsolutePath().toString();

      // start the script, optionally with parameters
      Process proc = new ProcessBuilder("python3", "-u", tempScriptFilePath, inputJson).start();
      logger.debug("Starting script " + tempScriptFilePath);
      this.addSubTask("Script started").complete();

      // catch the output
      try (BufferedReader bfr = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
        String line;
        while ((line = bfr.readLine()) != null) {
          this.addSubTask(line).complete();
        }
      }

      proc.waitFor();
      // Check for errors
      if (proc.exitValue() > 0) {
        this.setError("Script failed. Exit value: " + proc.exitValue());
      } else {
        this.complete();
      }
      logger.debug("Completed script " + tempScriptFilePath);
    } catch (Exception e) {
      this.setError("Script failed: " + e.getMessage());
      throw new MolgenisException("Script execution failed", e);
    }
  }
}
