package org.molgenis.emx2.beaconv2;

import java.util.*;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.*;

// todo: do we still need this if we use retrieveRows?
public class QueryHelper {

  private QueryHelper() {
    // static only
  }

  /**
   * Finalize GraphQL filter by adding missing end braces
   *
   * @param filter
   * @return
   */
  public static String finalizeFilter(String filter) {
    int nrOfStartingBraces = StringUtils.countMatches(filter, "{");
    return filter + "}".repeat(nrOfStartingBraces);
  }
}
