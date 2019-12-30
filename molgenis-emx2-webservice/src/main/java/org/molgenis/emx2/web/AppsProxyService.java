package org.molgenis.emx2.web;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static spark.Spark.get;

/**
 * On purpose we don't want here full fledged proxy because we don't want cookies etc to be passed.
 * We only proxy dumb resources
 */
public class AppsProxyService {
  static final Logger logger = LoggerFactory.getLogger(AppsProxyService.class);
  static final OkHttpClient client =
      new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE).build();
  static final String path = formatPath("/apps");
  static final String pathFilter = path + "*";

  // thanks to some inspiration from
  // https://github.com/abhinavsayan/java-spark-proxy-server/blob/master/src/main/java/server/ProxyServer.java

  public static void create(Database database) {
    Schema schema = database.getSchema("System");
    if (schema == null) schema = database.createSchema("System");
    SchemaMetadata settingsSchema = new SchemaMetadata();
    settingsSchema.create(
        new TableMetadata("Apps")
            .addColumn(new Column("path").pkey(true))
            .addColumn(new Column("source")));
    schema.merge(settingsSchema);
    // load some defaults
    schema
        .getTable("Apps")
        .update(
            new Row().set("path", "nu").set("source", "http://www.nu.nl"),
            new Row()
                .set("path", "molgenis-app-reports")
                .set("source", "http://unpkg.com/@mswertz/molgenis-app-reports@0.1.12/"));

    get(pathFilter, AppsProxyService::handleRequest);
  }

  private static Object handleRequest(Request req, Response res) throws IOException {
    // todo, cache
    Database database = sessionManager.getSession(req).getDatabase();
    Map<String, String> apps = new LinkedHashMap<>();
    for (Row r : database.getSchema("System").getTable("Apps").retrieve()) {
      apps.put(r.getString("path"), r.getString("source"));
    }

    // need to find first element of path beyond path filter
    String rawPath = getPath(req.url()).replaceFirst(path, "");
    String appName = rawPath.split("/")[0];
    if (apps.get(appName) == null || rawPath.equals(appName)) { // we require ending / in path
      res.status(404);
      StringBuilder appLinks = new StringBuilder();
      for (String app : apps.keySet()) {
        appLinks.append("<li><a href=\"" + formatPath(path + app) + "\">" + app + "</a></li>");
      }
      return "App with name '" + appName + "' unknown. Known apps: " + appLinks.toString();
    }

    String appBasePath = formatPath(path + appName);
    String targetBaseUrl = formatPath(apps.get(appName));
    String targetBasePath = getPath(targetBaseUrl);

    // setup the request
    String targetFullUrl = getURL(req, appBasePath, targetBaseUrl);
    logger.info("trying to proxy " + getPath(req.url()) + " to " + targetFullUrl.toString());

    // build request excluding headers
    okhttp3.Request proxyRequest = new okhttp3.Request.Builder().url(targetFullUrl).build();

    // execute the request
    okhttp3.Response proxyResponse = client.newCall(proxyRequest).execute();

    // map to the response
    if (proxyResponse.isSuccessful()) {
      mapResponseHeaders(proxyResponse, res);

      // if html or css than create body string
      if (proxyResponse.body().contentType().subtype().equals("html")
          || proxyResponse.body().contentType().subtype().equals("css")) {
        // rewrite local path
        res.body(rewriteHtml(proxyResponse.body().string(), targetBasePath, appBasePath));
      } else {
        // else transfer raw bytes
        // proxyResponse.body().byteStream().transferTo(res.raw().getOutputStream());
        res.body(proxyResponse.body().string());
      }
    } else {
      proxyResponse.body().byteStream().transferTo(res.raw().getOutputStream());
    }
    res.status(proxyResponse.code());

    // return result
    return res.raw();
  }

  private static String getPath(String url) {
    String temp;
    try {
      temp = new URL(url).getPath();
    } catch (MalformedURLException e) {
      throw new MolgenisException("Internal error with the proxy", e);
    }
    return temp;
  }

  private static Headers getHeaders(Request req) {
    Headers.Builder headers = new Headers.Builder();
    for (String headerName : req.headers()) {
      headers.add(headerName, req.headers(headerName));
    }
    return headers.build();
  }

  private static void mapResponseHeaders(okhttp3.Response proxyResponse, spark.Response res) {
    for (String headerName : proxyResponse.headers().names()) {
      res.header(headerName, proxyResponse.header(headerName));
    }
  }

  private static String formatPath(String path) {
    if (path == null) {
      path = "/";
    }
    if (!path.endsWith("/")) {
      path += "/";
    }
    return path;
  }

  private static String getURL(spark.Request req, String proxyPath, String proxyTarget) {
    return proxyTarget + req.pathInfo().replace(proxyPath, "");
    // return (req.queryString() == null) ? proxyUrl : (proxyUrl + "?" + req.queryString());

  }

  private static String rewriteHtml(String body, String oldPath, String newPath) {
    // sometimes you don't need '"' in html
    body = body.replaceAll("href\\s*=\\s*\\s*" + oldPath, "href=" + newPath);
    body = body.replaceAll("src\\s*=\\s*\\s*" + oldPath, "src=" + newPath);
    // normal code
    body = body.replaceAll("href\\s*=\\s*\"\\s*" + oldPath, "href=\"" + newPath);
    body = body.replaceAll("src\\s*=\\s*\"\\s*" + oldPath, "src=\"" + newPath);
    body = body.replaceAll("url\\s*\\(\\s*'\\s*" + oldPath, "url('" + newPath);
    return body;
  }
}
