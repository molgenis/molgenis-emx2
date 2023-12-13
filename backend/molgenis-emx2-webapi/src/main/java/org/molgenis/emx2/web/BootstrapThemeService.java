package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.logger;
import static spark.Spark.get;

import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import sass.embedded_protocol.EmbeddedSass;
import spark.Request;
import spark.Response;

public class BootstrapThemeService {
  private static final String HEX_WEBCOLOR_PATTERN = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$";
  private static final Pattern pattern = Pattern.compile(HEX_WEBCOLOR_PATTERN);
  private static Map<String, String> cache = new LinkedHashMap<>();

  private BootstrapThemeService() {
    // hide constructor
  }

  public static void create() {
    // per schema theme.css (later we will want to use settings here, right?)
    final String schemaPath = "/:schema/theme.css"; // NOSONAR
    get(schemaPath, BootstrapThemeService::getCss);
  }

  public static String getCss(Request request, Response response) {
    response.type("text/css");
    Map<String, String> params = getParams(request);

    // then override with url query, if any
    params.putAll(splitQuery(request.queryString()));

    // see if we have it cached, otherwise generate the css
    String key =
        params.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));
    if (cache.containsKey(key)) {
      return cache.get(key);
    } else {
      return generateCss(params);
    }
  }

  @NotNull
  private static Map<String, String> getParams(Request request) {
    Schema schema = getSchema(request);
    Map<String, String> params = new LinkedHashMap<>();
    if (schema != null) {
      Optional<String> cssUrl = schema.getMetadata().findSettingValue("cssURL");
      if (cssUrl.isPresent()) {
        params.putAll(splitQuery(cssUrl.get().split("\\?")[1]));
      }
    }
    return params;
  }

  private static Map<String, String> splitQuery(String query) {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(
            URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
      }
    }
    return query_pairs;
  }

  /**
   * Generate compressed CSS based on input colors and contents of theme.scss using an embedded SASS
   * compiler.
   */
  public static String generateCss(Map<String, String> params) {
    String primaryColor =
        getColor(params.get("primaryColor"), "#017FFD", "Primary color invalid: ");
    String menubarColor =
        getColor(params.get("menubarColor"), "#017FFD", "menubar color invalid: ");
    String input =
        String.format(
            "$theme-colors:(%nprimary: %s, %nmenubar: %s%n);%n%n", primaryColor, menubarColor);
    StringBuilder CssPlusScss = new StringBuilder();
    CssPlusScss.append(input);
    try {
      CssPlusScss.append(getScssFileContents("theme/theme.scss"));
      SassCompiler sassCompiler = SassCompilerFactory.bundled();
      sassCompiler.setOutputStyle(EmbeddedSass.OutputStyle.COMPRESSED);
      String cssCompiled = sassCompiler.compileScssString(CssPlusScss.toString()).getCss();
      sassCompiler.close();
      return cssCompiled;
    } catch (Exception e) {
      logger.error(String.format("SASS compilation failed, input was:%n%s", input));
      throw new MolgenisException("SASS compilation failed", e);
    }
  }

  /**
   * Based on the location of an SCSS file, grab all contents from that file, as well as recursively
   * following any @import links to append the contents those mports as well.
   */
  public static String getScssFileContents(String file) throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    InputStream fileFromResourceAsStream = getFileFromResourceAsStream(file);
    InputStreamReader inputStreamReader =
        new InputStreamReader(fileFromResourceAsStream, StandardCharsets.UTF_8);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      if (line.startsWith("@import")) {
        String importFileLocation =
            line.replace("@import", "").replace("\"", "").replace("'", "").replace(";", "").trim();
        if (!importFileLocation.endsWith(".scss")) {
          int lastFwdSlashIndex = importFileLocation.lastIndexOf("/");
          if (lastFwdSlashIndex == -1) {
            importFileLocation = "_" + importFileLocation + ".scss";
          } else {
            importFileLocation =
                importFileLocation.substring(0, lastFwdSlashIndex + 1)
                    + "_"
                    + importFileLocation.substring(lastFwdSlashIndex + 1)
                    + ".scss";
          }
        }
        String fileParent = file.substring(0, file.lastIndexOf("/"));
        String completeFileImportLocation = fileParent + "/" + importFileLocation;
        stringBuilder.append(getScssFileContents(completeFileImportLocation));
      } else {
        stringBuilder.append(line).append(System.lineSeparator());
      }
    }
    return stringBuilder.toString();
  }

  /** Get a file from the resources folder as an InputStream. */
  private static InputStream getFileFromResourceAsStream(String fileName) {
    ClassLoader classLoader = BootstrapThemeService.class.getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(fileName);
    if (inputStream == null) {
      throw new IllegalArgumentException("File not found: " + fileName);
    } else {
      return inputStream;
    }
  }

  private static String getColor(String customColor, String defaultColor, String errorMessage) {
    String primaryColor = customColor == null ? defaultColor : "#" + customColor;
    if (isValidColor(primaryColor)) {
      return primaryColor;
    } else {
      throw new MolgenisException(errorMessage + primaryColor);
    }
  }

  public static boolean isValidColor(final String colorCode) {
    Matcher matcher = pattern.matcher(colorCode);
    return matcher.matches();
  }
}
