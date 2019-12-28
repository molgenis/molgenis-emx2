package org.molgenis.emx2.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static spark.Spark.get;

public class UiProxy {
  static final Logger logger = LoggerFactory.getLogger(UiProxy.class);

  // thanks to some inspiration from
  // https://github.com/abhinavsayan/java-spark-proxy-server/blob/master/src/main/java/server/ProxyServer.java

  public static void enableProxy(String proxyPath, String proxyTarget) {

    // we only support get, might also need head and options
    // we certainly don't support post, put, patch and delete

    String path = formatPath(proxyPath);
    String target = formatPath(proxyTarget);
    String pathFilter = path + "*";

    get(
        pathFilter,
        (req, res) -> {
          // setup the request
          URL proxyUrl = getURL(req, path, target);
          logger.info("trying to proxy " + req.url() + " to " + proxyUrl.toString());

          HttpURLConnection proxyConnection = (HttpURLConnection) proxyUrl.openConnection();
          mapRequestHeaders(req, proxyConnection);
          proxyConnection.setRequestMethod("GET");

          // check for redirect
          int status = proxyConnection.getResponseCode();
          if (status == HttpURLConnection.HTTP_MOVED_TEMP
              || status == HttpURLConnection.HTTP_MOVED_PERM) {
            String location = proxyConnection.getHeaderField("Location");
            URL newUrl = new URL(location);
            proxyConnection = (HttpURLConnection) newUrl.openConnection();
            mapRequestHeaders(req, proxyConnection);
            proxyConnection.setRequestMethod("GET");
          }

          if (("" + status).startsWith("4")) {
            res.status(status);
            mapResponseHeaders(proxyConnection, res);
            proxyConnection.getErrorStream().transferTo(res.raw().getOutputStream());
          }

          // copy contents to response
          res.status(proxyConnection.getResponseCode());
          mapResponseHeaders(proxyConnection, res);
          // if html then rewrite
          if (isHtml(proxyConnection)) {
            String body = getBody(proxyConnection);
            String oldPath = new URL(target).getPath();
            body = rewriteHtml(body, oldPath, path);
            res.body(body);
          } else {
            // else push raw
            proxyConnection.getInputStream().transferTo(res.raw().getOutputStream());
          }
          proxyConnection.disconnect();

          // return result
          return res.raw();
        });
  }

  private static String getBody(HttpURLConnection conn) throws IOException {
    String contentEncoding = conn.getContentEncoding();
    if (contentEncoding == null) {
      contentEncoding = "utf-8";
    }
    InputStreamReader in = null;
    if ("gzip".equals(contentEncoding) || "br".equals(contentEncoding)) {
      in = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
    } else {
      in = new InputStreamReader((InputStream) conn.getContent());
    }
    BufferedReader buff = new BufferedReader(in);
    String line;
    StringBuilder builder = new StringBuilder();
    do {
      line = buff.readLine();
      builder.append(line).append("\n");
    } while (line != null);
    buff.close();
    return builder.toString();
  }

  private static boolean isHtml(HttpURLConnection con) {
    for (Map.Entry<String, List<String>> header : con.getHeaderFields().entrySet()) {
      if (header.getKey() != null && header.getKey().contains("Content-Type")) {
        for (String value : header.getValue()) {
          if (value.contains("html")) return true;
        }
      }
    }
    return false;
  }

  private static void mapRequestHeaders(Request request, HttpURLConnection con) {
    for (String header : request.headers()) {
      if (!header.equals("Content-Length")) {
        con.setRequestProperty(header, request.headers(header));
      }
    }
  }

  private static void mapResponseHeaders(HttpURLConnection con, Response res) {
    for (Map.Entry<String, List<String>> header : con.getHeaderFields().entrySet()) {
      logger.debug("header:" + header.getKey() + " " + header.getValue());
      if (header.getKey() != null) {
        res.header(header.getKey(), String.join(",", header.getValue()));
      }
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

  private static URL getURL(Request req, String proxyPath, String proxyTarget)
      throws MalformedURLException {
    String proxyUrl = proxyTarget + req.pathInfo().replace(proxyPath, "");
    return new URL((req.queryString() == null) ? proxyUrl : (proxyUrl + "?" + req.queryString()));
  }

  private static String rewriteHtml(String body, String oldPath, String newPath) {
    body = body.replaceAll("href\\s*=\\s*\"\\s*" + oldPath, "href=\"" + newPath);
    body = body.replaceAll("src\\s*=\\s*\"\\s*" + oldPath, "src=\"" + newPath);
    return body;
  }
}
