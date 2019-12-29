package org.molgenis.emx2.web;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.brotli.BrotliInterceptor;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import static spark.Spark.get;

/**
 * On purpose we don't want here full fledged proxy because we don't want cookies etc to be passed.
 * We only proxy dumb resources
 */
public class AppsProxy {
  static final Logger logger = LoggerFactory.getLogger(AppsProxy.class);
  static final OkHttpClient client =
      new OkHttpClient.Builder().addInterceptor(BrotliInterceptor.INSTANCE).build();

  // thanks to some inspiration from
  // https://github.com/abhinavsayan/java-spark-proxy-server/blob/master/src/main/java/server/ProxyServer.java

  public static void enableProxy(String proxyPath, Map<String, String> apps) {

    // we only support get, might also need head and options
    // we certainly don't support post, put, patch and delete

    String path = formatPath(proxyPath);
    String pathFilter = path + "*";

    get(
        pathFilter,
        (req, res) -> {
          // need to find first element of path beyond path filter
          String appName = getPath(req.url()).replaceFirst(path, "").split("/")[0];
          if (apps.get(appName) == null) {
            res.status(404);
            StringBuilder appLinks = new StringBuilder();
            for (String app : apps.keySet()) {
              appLinks.append(
                  "<li><a href=\"" + formatPath(path + app) + "\">" + app + "</a></li>");
            }
            return "App with name '" + appName + "' unknown. Known apps: " + appLinks.toString();
          }

          String appBasePath = formatPath(path + appName);
          String targetBaseUrl = formatPath(apps.get(appName));
          String targetBasePath = getPath(targetBaseUrl);

          // setup the request
          String targetFullUrl = getURL(req, appBasePath, targetBaseUrl);
          logger.info("trying to proxy " + req.url() + " to " + targetFullUrl.toString());

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
        });
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
    String proxyUrl = proxyTarget + req.pathInfo().replace(proxyPath, "");
    return (req.queryString() == null) ? proxyUrl : (proxyUrl + "?" + req.queryString());
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
