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
  private String name;
  private String script;
  private String outputFileExtension;
  private String parameters;
  private String token;

  public ScriptTask() {
    super("Starting script");
  }

  @Override
  public void run() {
    if (script == null) {
      this.setError("Script is required");
      return;
    }

    this.start();
    // temporary files with script and to collect output
    Path tempScriptFile = null;
    Path tempOutputFile = null;
    try {
      // paste the script to a file
      tempScriptFile = Files.createTempFile("python", ".py");
      tempOutputFile = Files.createTempFile("output", "." + outputFileExtension);
      String inputJson = parameters != null ? parameters : "{}";
      Files.write(tempScriptFile, this.script.getBytes(UTF_8), StandardOpenOption.WRITE);
      String tempScriptFilePath = tempScriptFile.toAbsolutePath().toString();

      // start the script, optionally with parameters
      ProcessBuilder builder = new ProcessBuilder("python3", "-u", tempScriptFilePath, inputJson);
      if (token != null) {
        builder.environment().put("MOLGENIS_TOKEN", token); // token for security use
      }
      builder
          .environment()
          .put(
              "OUTPUT_FILE",
              tempOutputFile.toAbsolutePath().toString()); // in case of an output file
      Process process = builder.start();
      logger.debug("Starting script " + tempScriptFilePath);
      this.addSubTask("Script started").complete();

      // catch the output
      try (BufferedReader bfr =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = bfr.readLine()) != null) {
          this.addSubTask(line).complete();
        }
      }

      process.waitFor();
      // Check for errors
      if (process.exitValue() > 0) {
        this.setError("Script failed. Exit value: " + process.exitValue());
      } else {
        // get any output file if exists
        if (Files.exists(tempOutputFile)) {
          this.handleOutput(tempOutputFile.toFile());
        }
        this.complete();
      }
      logger.debug("Completed script " + tempScriptFilePath);
    } catch (Exception e) {
      this.setError("Script failed: " + e.getMessage());
      throw new MolgenisException("Script execution failed", e);
    } finally {
      try {
        Files.deleteIfExists(tempScriptFile);
        Files.deleteIfExists(tempOutputFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getName() {
    return name;
  }

  public ScriptTask name(String name) {
    this.name = name;
    return this;
  }

  public String getScript() {
    return script;
  }

  public ScriptTask script(String script) {
    this.script = script;
    return this;
  }

  public String getParameters() {
    return parameters;
  }

  public ScriptTask parameters(String parameters) {
    this.parameters = parameters;
    return this;
  }

  public String getToken() {
    return token;
  }

  public ScriptTask token(String token) {
    this.token = token;
    return this;
  }

  public ScriptTask outputFileExtension(String outputFileExtension) {
    this.outputFileExtension = outputFileExtension;
    return this;
  }
}
