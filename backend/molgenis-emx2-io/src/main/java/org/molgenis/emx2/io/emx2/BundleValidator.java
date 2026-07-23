package org.molgenis.emx2.io.emx2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

public final class BundleValidator {

  private static final String ROOT = Emx2Yaml.MOLGENIS_YAML;
  private static final String KEY_BUNDLE = "bundle";
  private static final String KEY_PERMISSIONS = "permissions";
  private static final String KEY_MEMBERS = "members";
  private static final String ACCOUNT_MARKER = "@";

  private interface ContentLoader {
    String load(String path);
  }

  private BundleValidator() {}

  public static Emx2YamlBundle validate(Map<String, String> files) {
    Emx2YamlBundle bundle = Emx2Yaml.fromBundleFiles(files);
    validateCompanions(ROOT, files::get);
    return bundle;
  }

  public static Emx2YamlBundle validate(Path bundlePathOrDir) throws IOException {
    Path directory =
        Files.isDirectory(bundlePathOrDir) ? bundlePathOrDir : bundlePathOrDir.getParent();
    Emx2YamlBundle bundle = Emx2Yaml.fromBundle(directory);
    validateCompanions(ROOT, diskLoader(directory));
    return bundle;
  }

  private static ContentLoader diskLoader(Path directory) {
    return path -> {
      Path file = directory.resolve(path);
      if (!Files.exists(file)) {
        return null;
      }
      try {
        return Files.readString(file);
      } catch (IOException exception) {
        throw new MolgenisException("Failed to read companion bundle: " + path, exception);
      }
    };
  }

  private static void validateCompanions(String rootPath, ContentLoader loader) {
    String rootContent = loader.load(rootPath);
    if (rootContent == null) {
      return;
    }
    validateSchemaDeclarations(rootPath, rootContent);
    detectCompanionCycles(rootPath, loader);
  }

  private static void validateSchemaDeclarations(String fileLabel, String content) {
    YamlDocumentReader reader = new YamlDocumentReader(fileLabel);
    MappingNode schemas = schemasMapping(reader, content);
    if (schemas == null) {
      return;
    }
    for (NodeTuple entry : schemas.getValue()) {
      String schemaName = reader.scalar(entry.getKeyNode(), Emx2Yaml.KEY_ADDITIONAL_SCHEMAS);
      if (entry.getValueNode() instanceof MappingNode body) {
        validateSchemaEntry(reader, schemaName, body);
      }
    }
  }

  private static void validateSchemaEntry(
      YamlDocumentReader reader, String schemaName, MappingNode body) {
    for (NodeTuple tuple : body.getValue()) {
      String key = reader.scalar(tuple.getKeyNode(), Emx2Yaml.KEY_ADDITIONAL_SCHEMAS);
      if (KEY_MEMBERS.equals(key)) {
        throw memberError(reader, schemaName, tuple.getKeyNode());
      }
      if (KEY_PERMISSIONS.equals(key)) {
        validatePermissions(reader, schemaName, tuple.getValueNode());
      }
    }
  }

  private static void validatePermissions(
      YamlDocumentReader reader, String schemaName, Node permissionsNode) {
    if (!(permissionsNode instanceof MappingNode permissions)) {
      return;
    }
    for (NodeTuple tuple : permissions.getValue()) {
      String role = reader.scalar(tuple.getKeyNode(), KEY_PERMISSIONS);
      if (KEY_MEMBERS.equals(role)) {
        throw memberError(reader, schemaName, tuple.getKeyNode());
      }
      if (tuple.getValueNode() instanceof ScalarNode grantee
          && grantee.getValue().contains(ACCOUNT_MARKER)) {
        throw reader.error(
            "companion schema '"
                + schemaName
                + "' grants '"
                + role
                + "' to the member account '"
                + grantee.getValue()
                + "'; a bundle may declare role-default permissions only, never member accounts",
            tuple.getValueNode());
      }
    }
  }

  private static MolgenisException memberError(
      YamlDocumentReader reader, String schemaName, Node node) {
    return reader.error(
        "companion schema '"
            + schemaName
            + "' declares member accounts ('"
            + KEY_MEMBERS
            + "'); a bundle may declare role-default permissions only, never member accounts",
        node);
  }

  private static void detectCompanionCycles(String rootPath, ContentLoader loader) {
    walk(rootPath, loader, new LinkedHashSet<>(), new HashSet<>());
  }

  private static void walk(
      String bundlePath, ContentLoader loader, Set<String> inStack, Set<String> done) {
    inStack.add(bundlePath);
    String content = loader.load(bundlePath);
    if (content != null) {
      for (String companion : companionBundlePaths(bundlePath, content)) {
        if (inStack.contains(companion)) {
          throw new MolgenisException(
              "companion schema cycle detected: '"
                  + companion
                  + "' is referenced within its own companion chain");
        }
        if (!done.contains(companion)) {
          walk(companion, loader, inStack, done);
        }
      }
    }
    inStack.remove(bundlePath);
    done.add(bundlePath);
  }

  private static List<String> companionBundlePaths(String bundlePath, String content) {
    YamlDocumentReader reader = new YamlDocumentReader(bundlePath);
    MappingNode schemas = schemasMapping(reader, content);
    List<String> companions = new ArrayList<>();
    if (schemas == null) {
      return companions;
    }
    for (NodeTuple entry : schemas.getValue()) {
      if (entry.getValueNode() instanceof MappingNode body) {
        Node bundleRef = valueOf(body, KEY_BUNDLE);
        if (bundleRef instanceof ScalarNode scalar) {
          companions.add(resolveRelative(bundlePath, scalar.getValue()));
        }
      }
    }
    return companions;
  }

  private static MappingNode schemasMapping(YamlDocumentReader reader, String content) {
    Node rootNode = reader.compose(content);
    if (rootNode instanceof MappingNode rootMapping
        && valueOf(rootMapping, Emx2Yaml.KEY_ADDITIONAL_SCHEMAS) instanceof MappingNode schemas) {
      return schemas;
    }
    return null;
  }

  private static Node valueOf(MappingNode mapping, String key) {
    for (NodeTuple tuple : mapping.getValue()) {
      if (tuple.getKeyNode() instanceof ScalarNode scalar && key.equals(scalar.getValue())) {
        return tuple.getValueNode();
      }
    }
    return null;
  }

  private static String resolveRelative(String referrerPath, String reference) {
    Deque<String> parts = new ArrayDeque<>();
    int lastSlash = referrerPath.lastIndexOf('/');
    if (lastSlash >= 0) {
      for (String part : referrerPath.substring(0, lastSlash).split("/")) {
        if (!part.isEmpty()) {
          parts.addLast(part);
        }
      }
    }
    for (String part : reference.split("/")) {
      if (part.isEmpty() || ".".equals(part)) {
        continue;
      }
      if ("..".equals(part)) {
        if (!parts.isEmpty()) {
          parts.removeLast();
        }
      } else {
        parts.addLast(part);
      }
    }
    return String.join("/", parts);
  }
}
