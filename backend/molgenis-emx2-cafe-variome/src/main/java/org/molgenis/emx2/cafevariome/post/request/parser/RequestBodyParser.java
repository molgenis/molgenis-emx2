package org.molgenis.emx2.cafevariome.post.request.parser;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Because of repeated parameters for multiple values we need to parse it ourselves, i.g.
 *
 * <p>jsonAPI[query][components][sim][0][ids][]=HP:0000175&
 * jsonAPI[query][components][sim][0][ids][]=HP:0009902&
 * jsonAPI[query][components][sim][0][ids][]=HP:0030690&
 */
public class RequestBodyParser {

  public RequestBodyParser() {}

  /**
   * Parse request, special bit is the concatenation of repeated parameters
   *
   * @param reqBody
   * @return
   * @throws Exception
   */
  public static Map<String, String> parse(String reqBody) throws Exception {
    Map<String, String> result = new HashMap<>();
    String[] splitReqBody = reqBody.split("&", -1);
    for (String splitReqBodyStr : splitReqBody) {
      splitReqBodyStr = java.net.URLDecoder.decode(splitReqBodyStr, StandardCharsets.UTF_8.name());
      String[] splitParamVal = splitReqBodyStr.split("=", -1);
      if (splitParamVal.length != 2) {
        throw new Exception("Bad param/value in request");
      }
      String param = splitParamVal[0];
      String val = splitParamVal[1];
      if (result.containsKey(param)) {
        if (val.contains(",")) {
          throw new Exception("Must comma concat repeated values but already contains a comma");
        }
        result.put(param, result.get(param) + "," + val);
      } else {
        result.put(param, val);
      }
    }
    return result;
  }
}
