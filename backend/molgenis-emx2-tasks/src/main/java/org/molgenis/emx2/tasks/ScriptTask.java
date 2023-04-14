package org.molgenis.emx2.tasks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptTask extends Task {
  private static final Logger logger = LoggerFactory.getLogger(ScriptTask.class);
  private String name;
  private String script;
  private String outputFileExtension;
  private String parameters;
  private String token;
  private String dependencies;

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
    Path tempDir = null;
    try {
      try {
        // create tmp directory
        tempDir = Files.createTempDirectory("python");
        this.addSubTask("Created temp directory").complete();

        // paste the script to a file into temp dir
        Path tempScriptFile = Files.createFile(tempDir.resolve("script.py"));
        Files.writeString(tempScriptFile, this.script);
        Path requirementsFile = Files.createFile(tempDir.resolve("requirements.txt"));
        Files.writeString(requirementsFile, this.dependencies != null ? this.dependencies : "");

        // define commands (given tempDir as working directory)
        String createVenvCommand = "python3 -m venv venv";
        String activateCommand = "source venv/bin/activate";
        String installRequirementsCommand = "pip3 install -r requirements.txt";
        String runScriptCommand = "python3 -u script.py";

        // define outputFile and inputJson
        Path tempOutputFile = Files.createTempFile(tempDir, "output", "." + outputFileExtension);
        String inputJson = parameters != null ? parameters : "{}";

        // start the script
        ProcessBuilder builder =
            new ProcessBuilder(
                    "bash",
                    "-c",
                    createVenvCommand
                        + " && "
                        + activateCommand
                        + " && "
                        + installRequirementsCommand
                        + " && "
                        + runScriptCommand)
                .directory(tempDir.toFile());
        if (token != null) {
          builder.environment().put("MOLGENIS_TOKEN", token); // token for security use
        }
        builder
            .environment()
            .put(
                "OUTPUT_FILE",
                tempOutputFile.toAbsolutePath().toString()); // in case of an output file
        Process process = builder.start();
        this.addSubTask("Script started").complete();

        // catch the output
        try (BufferedReader bfr =
            new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = bfr.readLine()) != null) {
            this.addSubTask(line).complete();
          }
        }

        // catch the error
        String error;
        try (BufferedReader bufferedReader =
            new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
          error = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        if (error != null) {
          this.addSubTask("Script complete").setError(error);
        }
        process.waitFor();
        // Check for errors
        if (process.exitValue() > 0) {
          this.setError("Script failed. Exit value: " + process.exitValue());
        } else {
          // get any output file if exists
          if (Files.exists(tempOutputFile) && Files.size(tempOutputFile) > 0) {
            this.handleOutput(tempOutputFile.toFile());
          }
          this.complete();
        }
      } finally {
        if (tempDir != null) {
          Files.walk(tempDir)
              .sorted(Comparator.reverseOrder())
              .map(Path::toFile)
              .forEach(File::delete);
          if (Files.exists(tempDir)) {
            throw new MolgenisException("temp dir still exists");
          }
        }
      }
    } catch (InterruptedException ie) {
      // should not happen
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      this.setError("Script failed: " + e.getMessage());
      throw new MolgenisException("Script execution failed", e);
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

  public ScriptTask dependencies(String dependencies) {
    this.dependencies = dependencies;
    return this;
  }
}
