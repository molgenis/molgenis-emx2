package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static org.molgenis.emx2.web.MolgenisWebservice.logger;
import static spark.Spark.get;

import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Import;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import spark.Request;
import spark.Response;

public class BootstrapThemeService {
  private static final Map<String, String> cache = new LinkedHashMap<>();
  private static final String HEX_WEBCOLOR_PATTERN = "^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})$";
  private static final Pattern pattern = Pattern.compile(HEX_WEBCOLOR_PATTERN);

  private BootstrapThemeService() {
    // hide constructor
  }

  public static void create() {
    // per schema theme.css (later we will want to use settings here, right?)
    final String schemaPath = "/:schema/theme.css"; // NOSONAR
    get(schemaPath, BootstrapThemeService::getCss);
  }

  public static String getCss(Request request, Response response)
      throws UnsupportedEncodingException {
    response.type("text/css");

    Map<String, String> params = new LinkedHashMap<>();

    // first get params from schema settings, if exists
    Schema schema = getSchema(request);
    if (schema != null) {
      String cssUrl = schema.getMetadata().getSetting("cssURL");

      if (cssUrl != null) {
        params.putAll(splitQuery(cssUrl.split("\\?")[1]));
      }
    }

    // then override with url query, if any
    params.putAll(splitQuery(request.queryString()));

    // see if we have it cached, otherwise generate the css
    String key =
        params.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));

    return cache.computeIfAbsent(key, k -> generateCss(params));
  }

  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<>();
    if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(
            URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
    }
    return query_pairs;
  }

  public static String generateCss(Map<String, String> params) {
    // prepare the params with defaults
    String primary = params.get("primary") == null ? "#017FFD" : "#" + params.get("primary");
    if (!isValid(primary)) throw new MolgenisException("Primary color invalid: " + primary);
    String secondary = params.get("secondary") == null ? "#005EC4" : "#" + params.get("secondary");
    if (!isValid(secondary)) throw new MolgenisException("Secondary color invalid: " + primary);

    String input =
        String.format("$theme-colors:(\nprimary: %s, \nsecondary: %s\n);\n\n", primary, secondary);

    try {
      input += "@import \"theme.scss\"";
      Compiler compiler = new Compiler();
      Options options = new Options();
      options.setImporters(Collections.singleton(BootstrapThemeService::doImport));

      // example here
      // https://gitlab.com/jsass/jsass/blob/master/example/webapp/src/main/java/io/bit3/jsass/example/webapp/JsassServlet.java#L89-192

      Output output = compiler.compileString(input, options);
      return output.getCss();
    } catch (Exception e) {
      logger.error("SASS compilation failed, input was:\n" + input);
      throw new MolgenisException("SASS compilation failed", e);
    }
  }

  public static boolean isValid(final String colorCode) {
    Matcher matcher = pattern.matcher(colorCode);
    return matcher.matches();
  }

  /**
   * Resolve the target file for an {@code @import} directive.
   *
   * @param url The {@code import} url.
   * @param previous The file that contains the {@code import} directive.
   * @return The resolve import objects or {@code null} if the import file was not found.
   */
  private static Collection<Import> doImport(String url, Import previous) {
    try {

      // find in import paths
      final List<Path> importPaths = new LinkedList<>();

      // (a) relative to the previous import file
      String previousPath = previous.getAbsoluteUri().getPath();
      Path previousParentPath = Paths.get(previousPath).getParent();
      // inside jar we have relative path
      if (BootstrapThemeService.class
          .getClassLoader()
          .getResource("theme")
          .getPath()
          .contains("!")) {
        previousParentPath = Paths.get(previousPath.replaceFirst("/", "")).getParent();
      }
      if (previousParentPath != null) {
        importPaths.add(previousParentPath);
      }

      // (b) or simply in the root folder
      importPaths.add(Path.of("theme"));

      for (Path importPath : importPaths) {
        Path target = importPath.resolve(url);

        Collection<Import> imports = resolveImport(target);
        if (null != imports) {
          return imports;
        }
      }

      // file not found
      throw new FileNotFoundException(url);
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
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

    if (null == resource) {
      return null;
    }

    // calculate a jar absolute URI
    final String source = IOUtils.toString(resource, StandardCharsets.UTF_8);

    final URI absoluteIRI =
        resource.toString().contains("!")
            ? Paths.get("/").resolve(resource.toURI().toString().split("!")[1]).toUri()
            : resource.toURI();

    final Import scssImport = new Import(absoluteIRI, absoluteIRI, source);
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
  private static URL resolveResource(Path path) throws MalformedURLException {
    final Path dir = path.getParent();
    final String basename = path.getFileName().toString();

    for (String prefix : new String[] {"_", ""}) {
      for (String suffix : new String[] {".scss", ".css", ""}) {
        final Path target = dir.resolve(prefix + basename + suffix);
        URL resource = BootstrapThemeService.class.getClassLoader().getResource(target.toString());

        if (null != resource) {
          return resource;
        } else {
          // when running tests we are not inside a jar we test
          Path fullPath = Paths.get(target.toString());
          if (Files.exists(fullPath)) {
            return Paths.get(target.toString()).toUri().toURL();
          }
        }
      }
    }
    return null;
  }
}
