package org.molgenis.emx2.web;

import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.importer.Import;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import spark.Request;
import spark.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.logger;
import static spark.Spark.get;

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
      String cssUrl = schema.getMetadata().getSetting("cssURL");
      if (cssUrl != null) {
        params.putAll(splitQuery(cssUrl.split("\\?")[1]));
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

  public static String generateCss(Map<String, String> params) {
    String primaryColor =
        getColor(params.get("primaryColor"), "#017FFD", "Primary color invalid: ");
    String secondaryColor =
        getColor(params.get("secondaryColor"), "#005EC4", "Secondary color invalid: ");

    String input =
        String.format(
            "$theme-colors:(%nprimary: %s, %nsecondary: %s%n);%n%n@import 'theme.scss'",
            primaryColor, secondaryColor);
    Compiler compiler;
    try {
      // new Compiler() fails with java.lang.UnsatisfiedLinkError or java.lang.NoClassDefFoundError
      // if the required OS dependencies (system libraries or NativeAdapter) cannot be resolved
      compiler = new Compiler();
      Options options = new Options();
      options.setImporters(Collections.singleton(BootstrapThemeService::doScssImport));
      return compiler.compileString(input, options).getCss();
    } catch (Throwable e) {
      logger.error(e.getMessage());
      logger.error(String.format("SASS compilation failed, input was:%n%s", input));
      return retrieveThemeFromServer();
    }
  }

  /**
   * If local compilation of the theme CSS fails, retrieve a precompiled default theme CSS from an
   * online EMX2 instance. If the retrieval also fails, log the error and return null, resulting in
   * no theme available for styling the app.
   *
   * @return String containing theme CSS for styling the EMX2 app, or null if retrieval fails.
   * @throws IOException
   */
  public static String retrieveThemeFromServer() {
    try {
      return new Scanner(
              new URL("https://emx2.test.molgenis.org/apps/central/theme.css").openStream(),
              "UTF-8")
          .useDelimiter("\\A")
          .next();
    } catch (Exception e) {
      logger.error(
          "Fallback theme could not be retrieved because device is not connected to internet or server is unreachable.");
      logger.error(e.getMessage());
      return null;
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

  /**
   * Resolve the target file for an {@code @import} directive.
   *
   * @param fileName The {@code import} fileName.
   * @param previousFile The file that contains the {@code import} directive.
   * @return The resolve import objects or {@code null} if the import file was not found.
   */
  private static Collection<Import> doScssImport(String fileName, Import previousFile) {
    try {
      final Path importPath = getImportPath(previousFile);
      Path targetPath = importPath.resolve(fileName);
      return resolveImport(targetPath);
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  private static Path getImportPath(Import previous) {
    Path previousParentPath = getPreviousParentPath(previous);
    return Objects.requireNonNullElseGet(previousParentPath, () -> Path.of("theme"));
  }

  private static Path getPreviousParentPath(Import previous) {
    String previousPath = getPreviousPath(previous);
    return Paths.get(previousPath).getParent();
  }

  @NotNull
  private static String getPreviousPath(Import previous) {
    String absoluteUri = previous.getAbsoluteUri().toString();
    if (absoluteUri.contains("!")) {
      return absoluteUri.split("!/")[1];
    } else {
      return absoluteUri;
    }
  }

  /**
   * Try to determine the import object for a given path.
   *
   * @param path The path to resolve.
   * @return The import object or {@code null} if the file was not found.
   */
  private static Collection<Import> resolveImport(Path path)
      throws IOException, URISyntaxException {
    URL resource = resolveResource(path);
    final String source = IOUtils.toString(resource, StandardCharsets.UTF_8);
    final Import scssImport = new Import(resource.toURI(), resource.toURI(), source);
    return Collections.singleton(scssImport);
  }

  /**
   * Try to find a resource for this path.
   *
   * <p>A sass import like {@code @import "foo"} does not contain the partial prefix (underscore) or
   * file extension. This method will try the following namings to find the import file {@code foo}:
   *
   * <ul>
   *   <li>_foo.scss
   *   <li>_foo.css
   *   <li>_foo
   *   <li>foo.scss
   *   <li>foo.css
   *   <li>foo
   * </ul>
   *
   * @param path The path to resolve.
   * @return The resource URL of the resolved file or {@code null} if the file was not found.
   */
  private static URL resolveResource(Path path)
      throws FileNotFoundException, MalformedURLException {
    final Path dir = path.getParent();
    final String basename = path.getFileName().toString();

    for (String prefix : new String[] {"_", ""}) {
      for (String suffix : new String[] {".scss", ".css", ""}) {
        final Path target = dir.resolve(prefix + basename + suffix);
        URL resource = getResource(target);
        if (resource != null) {
          return resource;
        }
      }
    }
    throw new FileNotFoundException(basename);
  }

  private static URL getResource(Path target) throws MalformedURLException {
    URL resourceUrl =
        BootstrapThemeService.class
            .getClassLoader()
            .getResource(target.toString().replace('\\', '/'));

    if (resourceUrl == null) {
      return getNonJarUrlForTest(target);
    } else {
      return resourceUrl;
    }
  }

  private static URL getNonJarUrlForTest(Path target) throws MalformedURLException {
    Path fullPath = Paths.get(target.toString());
    if (Files.exists(fullPath)) {
      return Paths.get(target.toString()).toUri().toURL();
    } else {
      return null;
    }
  }
}
