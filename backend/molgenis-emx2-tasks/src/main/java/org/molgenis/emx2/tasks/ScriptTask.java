package org.molgenis.emx2.tasks;

import static org.apache.commons.text.StringEscapeUtils.escapeXSI;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.email.EmailMessage;
import org.molgenis.emx2.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptTask extends Task {
  private static Logger logger = LoggerFactory.getLogger(ScriptTask.class);
  private String name;
  private ScriptType type;
  private String script;
  private String outputFileExtension;
  private String parameters;
  private String token;
  private String dependencies;
  private HashMap<String, Object> extraFile;
  private Process process;
  private byte[] output;
  private URL serverUrl;

  private static final String EXTRA_FILE = "extraFile";
  private static final String EXTRA_FILE_FILENAME = EXTRA_FILE + "_filename";
  private static final String EXTRA_FILE_CONTENTS = EXTRA_FILE + "_contents";
  private static final String EXTRA_FILE_EXTENSION = EXTRA_FILE + "_extension";
  private static final String EXTRA_FILE_MIMETYPE = EXTRA_FILE + "_mimetype";
  private static final String EXTRA_FILE_SIZE = EXTRA_FILE + "_size";

  public ScriptTask(String name) {
    super("Executing script '" + name + "'");
    this.name = name;
  }

  public ScriptTask(Row scriptMetadata) {
    this(scriptMetadata.getString("name"));
    this.type(ScriptType.valueOf(scriptMetadata.getString("type").toUpperCase()))
        .script(scriptMetadata.getString("script"))
        .outputFileExtension(scriptMetadata.getString("outputFileExtension"))
        .dependencies(scriptMetadata.getString("dependencies"))
        .extraFile(scriptMetadata)
        .cronExpression(scriptMetadata.getString("cron"))
        .cronUserName(scriptMetadata.getString("cronUser"))
        .failureAddress(scriptMetadata.getString("failureAddress"))
        .disabled(
            !scriptMetadata.isNull("disabled", ColumnType.BOOL)
                && scriptMetadata.getBoolean("disabled"));
  }

  @Override
  public void run() {
    if (getStatus().equals(TaskStatus.ERROR)) {
      // when already errored before start, e.g. on unschedule
      return;
    }
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
        tempDir = Files.createTempDirectory("script_tasks"); // NOSONAR
        this.addSubTask("Created temp directory").complete();

        String command =
            switch (type) {
              case PYTHON -> createShellScriptToExecutePythonScript(tempDir);
              case BASH -> this.script;
            };

        // define outputFile and inputJson
        Path tempOutputFile = Files.createTempFile(tempDir, "output", "." + outputFileExtension);
        // start the script
        ProcessBuilder builder =
            new ProcessBuilder("bash", "-c", command).directory(tempDir.toFile()); // NOSONAR

        if (token != null) {
          builder.environment().put("MOLGENIS_TOKEN", token); // token for security use
        }
        builder
            .environment()
            .put(
                "OUTPUT_FILE",
                tempOutputFile.toAbsolutePath().toString()); // in case of an output file

        process = builder.start();
        this.addSubTask("Script started: " + process.info().commandLine().orElse("")).complete();

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
        if (!error.trim().isEmpty()) {
          this.addSubTask("Script complete with error").setError(error);
        }
        process.waitFor();
        // Check for errors
        if (process.exitValue() > 0) {
          this.setError("Script failed. Exit value: " + process.exitValue());
        } else {
          // get any output file if exists
          if (Files.exists(tempOutputFile) && Files.size(tempOutputFile) > 0) {
            this.handleOutput(tempOutputFile.toFile());
            this.output = Files.readAllBytes(tempOutputFile);
          }
          this.complete();
        }
      } finally {
        if (tempDir != null) {
          try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
          }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      this.setError("Script failed: " + e.getMessage());
      throw new MolgenisException("Script execution failed", e);
    } finally {
      if (getStatus() == TaskStatus.ERROR) {
        this.sendFailureMail();
      }
    }
  }

  private String createShellScriptToExecutePythonScript(Path tempDir) throws IOException {
    // paste the script to a file into temp dir
    Path tempScriptFile = Files.createFile(tempDir.resolve("script.py"));
    script = script.replace("${jobId}", this.getId());
    Files.writeString(tempScriptFile, this.script);
    Path requirementsFile = Files.createFile(tempDir.resolve("requirements.txt"));
    Files.writeString(requirementsFile, this.dependencies != null ? this.dependencies : "");

    if (this.extraFile != null && this.extraFile.get(EXTRA_FILE) != null) {
      String extraFileName = this.extraFile.get(EXTRA_FILE_FILENAME).toString();
      List<String> forbiddenFiles = Arrays.asList("venv.zip", "requirements.txt", "script.py");
      if (forbiddenFiles.contains(extraFileName)) {
        throw new MolgenisException(
            "Invalid file name '"
                + extraFileName
                + "'. "
                + "Ensure the name of the extra file is not any of 'script.py', 'requirements.txt', or 'venv.zip'.");
      }
      byte[] extraFileContent = (byte[]) this.extraFile.get(EXTRA_FILE_CONTENTS);
      Object extraFileExtension = this.extraFile.get(EXTRA_FILE_EXTENSION);
      Path extraFilePath = tempDir.resolve(extraFileName);

      try (FileOutputStream fos = new FileOutputStream(extraFilePath.toFile())) {
        fos.write(extraFileContent);
      }
      if (extraFileExtension != null && extraFileExtension.toString().equalsIgnoreCase("zip")) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(extraFilePath.toFile()))) {
          byte[] buffer = new byte[1024];
          ZipEntry entry;
          while ((entry = zis.getNextEntry()) != null) {
            File newFile = new File(tempDir.toFile(), entry.getName());
            if (entry.isDirectory()) {
              newFile.mkdirs();
            } else {
              new File(newFile.getParent()).mkdirs();
              try (FileOutputStream fos = new FileOutputStream(newFile)) {
                int length;
                while ((length = zis.read(buffer)) > 0) {
                  fos.write(buffer, 0, length);
                }
              }
            }
          }
        }
      }
    }

    // define commands (given tempDir as working directory)
    String createVenvCommand = "python3 -m venv venv";
    String activateCommand = "source venv/bin/activate";
    String pipUpgradeCommand = "pip3 install --upgrade pip";
    String installRequirementsCommand =
        "pip3 install -r requirements.txt --quiet"; // don't check upgrade
    String runScriptCommand = "python3 -u script.py";
    String escapedParameters = " " + escapeXSI(this.parameters);

    return createVenvCommand
        + " && "
        + activateCommand
        + " && "
        + pipUpgradeCommand
        + " && "
        + installRequirementsCommand
        + " && "
        + runScriptCommand
        + escapedParameters;
  }

  private void sendFailureMail() {
    if (this.getFailureAddress() != null && !this.getFailureAddress().isEmpty()) {
      EmailService emailService = new EmailService();
      String subject =
          "Molgenis script %s failed on %s".formatted(this.getName(), this.getServerUrl());
      String text =
          """
            Molgenis script %s failed with error:
            %s

            %s"""
              .formatted(this.getName(), this.getDescription(), this.getJobUrl());
      EmailMessage emailMessage =
          new EmailMessage(List.of(this.getFailureAddress()), subject, text, Optional.empty());
      emailService.send(emailMessage);
    }
  }

  public String getName() {
    return name;
  }

  public ScriptTask type(ScriptType type) {
    this.type = type;
    return this;
  }

  public ScriptType getType() {
    return type;
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

  @JsonIgnore
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

  public ScriptTask extraFile(Row scriptMetaData) {
    this.extraFile = new HashMap<>();
    this.extraFile.put(EXTRA_FILE, scriptMetaData.getString(EXTRA_FILE));
    this.extraFile.put(EXTRA_FILE_MIMETYPE, scriptMetaData.getString(EXTRA_FILE_MIMETYPE));
    this.extraFile.put(EXTRA_FILE_FILENAME, scriptMetaData.getString(EXTRA_FILE_FILENAME));
    this.extraFile.put(EXTRA_FILE_EXTENSION, scriptMetaData.getString(EXTRA_FILE_EXTENSION));
    this.extraFile.put(EXTRA_FILE_SIZE, scriptMetaData.getString(EXTRA_FILE_SIZE));
    this.extraFile.put(EXTRA_FILE_CONTENTS, scriptMetaData.getBinary(EXTRA_FILE_CONTENTS));
    return this;
  }

  @Override
  public void stop() {
    if (this.process != null && this.process.isAlive()) {
      this.process.destroy();
      this.logger.warn("stopping script " + name);
    }
    this.setError("process has been stopped");
  }

  @JsonIgnore
  public byte[] getOutput() {
    return this.output;
  }

  private String getJobUrl() {
    if (serverUrl != null) {
      return this.serverUrl.toExternalForm()
          + "/"
          + Constants.SYSTEM_SCHEMA
          + "/tasks/#/jobs?id="
          + this.getId();
    }
    return null;
  }

  public ScriptTask setServerUrl(URL url) {
    this.serverUrl = url;
    return this;
  }

  public URL getServerUrl() {
    return serverUrl;
  }
}
