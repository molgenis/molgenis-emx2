package org.molgenis.emx2.cafevariome.request.parameters;

public class EAVQueryParameters {

  public static String EAV_ATTRIBUTE(int nr) {
    return "jsonAPI[query][components][eav][" + nr + "][attribute]";
  }

  public static String EAV_OPERATOR(int nr) {
    return "jsonAPI[query][components][eav][" + nr + "][operator]";
  }

  public static String EAV_VALUE(int nr) {
    return "jsonAPI[query][components][eav][" + nr + "][value]";
  }
}
