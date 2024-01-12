package org.molgenis.emx2.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import org.apache.commons.lang3.SystemUtils;
import org.molgenis.emx2.MolgenisException;

public class FileUtils {
  private FileUtils() {
    // hide constructor
  }

  public static File getTempFile(String prefix, String suffix) throws IOException {
    File tempFile;
    if (SystemUtils.IS_OS_UNIX) {
      FileAttribute<Set<PosixFilePermission>> attr =
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
      tempFile = Files.createTempFile(prefix, suffix, attr).toFile();
    } else {
      tempFile = Files.createTempFile(prefix, suffix).toFile(); // NOSONAR
    }
    if (!tempFile.setReadable(true, true)
        || !tempFile.setWritable(true, true)
        || !tempFile.setExecutable(true, true)) {
      throw new MolgenisException("Internal error: create temp file failed");
    }
    return tempFile;
  }
}
