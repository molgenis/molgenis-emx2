package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathValidatorTest {

  @TempDir Path tempDir;
  Path bundleDir;

  @BeforeEach
  void setUp() throws IOException {
    bundleDir = tempDir.resolve("bundle");
    Files.createDirectories(bundleDir);
    Files.createDirectories(bundleDir.resolve("transforms"));
    Files.writeString(bundleDir.resolve("transforms/valid.jslt"), ".");
  }

  @Test
  void testValidPathWithinBundle() {
    Path result = PathValidator.validateWithinBase(bundleDir, "transforms/valid.jslt");
    assertTrue(result.toString().endsWith("transforms/valid.jslt"));
  }

  @Test
  void testPathTraversalBlocked() {
    FairMapperException ex =
        assertThrows(
            FairMapperException.class,
            () -> PathValidator.validateWithinBase(bundleDir, "../../../etc/passwd"));

    assertTrue(ex.getMessage().contains("escapes base directory"));
  }

  @Test
  void testNormalizedTraversalBlocked() {
    FairMapperException ex =
        assertThrows(
            FairMapperException.class,
            () -> PathValidator.validateWithinBase(bundleDir, "transforms/../../../etc/passwd"));

    assertTrue(ex.getMessage().contains("escapes base directory"));
  }

  @Test
  void testSymlinkTraversalBlocked() throws IOException {
    Path outsideFile = tempDir.resolve("outside.txt");
    Files.writeString(outsideFile, "secret");

    Path symlinkPath = bundleDir.resolve("symlink.txt");
    try {
      Files.createSymbolicLink(symlinkPath, outsideFile);

      FairMapperException ex =
          assertThrows(
              FairMapperException.class,
              () -> PathValidator.validateWithinBase(bundleDir, "symlink.txt"));

      assertTrue(ex.getMessage().contains("escapes base directory"));
    } catch (UnsupportedOperationException e) {
      System.out.println("Symlinks not supported on this OS, skipping test");
    }
  }

  @Test
  void testRelativePathWithDotsAllowedIfStaysInside() {
    Path result =
        PathValidator.validateWithinBase(bundleDir, "transforms/../transforms/valid.jslt");
    assertTrue(result.toString().endsWith("transforms/valid.jslt"));
  }
}
