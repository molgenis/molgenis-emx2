package org.molgenis.emx2.semantics.graphgenome;

import static org.molgenis.emx2.semantics.graphgenome.GraphGenome.DNA_PADDING;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RetrieveRefSeq {

  /** Get DNA reference from UCSC API. If that fails, return N-repeat sequence as fallback. */
  public static String getDnaFromUCSC(
      String ucscgenome,
      String chromosome,
      Long earliestStart,
      Long latestEnd,
      boolean offlineMode) {
    if (offlineMode) {
      return offlineDefaultSequence(earliestStart, latestEnd);
    }
    try {
      String chromosomeWithChr = chromosome.startsWith("chr") ? chromosome : "chr" + chromosome;
      String UCSCResponseStr =
          RetrieveRefSeq.httpGet(
              "https://api.genome.ucsc.edu/getData/sequence?genome="
                  + ucscgenome
                  + ";chrom="
                  + chromosomeWithChr
                  + ";start="
                  + (earliestStart - DNA_PADDING)
                  + ";end="
                  + (latestEnd + DNA_PADDING));
      UCSCAPIResponse UCSCResponse =
          new ObjectMapper().readValue(UCSCResponseStr, UCSCAPIResponse.class);
      return UCSCResponse.getDna();
    } catch (Exception e) {
      return offlineDefaultSequence(earliestStart, latestEnd);
    }
  }

  /** Print sequence of N bases (="any DNA base") to replace reference DNA when offline */
  public static String offlineDefaultSequence(Long earliestStart, Long latestEnd) {
    return "N".repeat((int) (latestEnd - earliestStart) + (DNA_PADDING * 2));
  }

  /**
   * Simple function to perform an HTTP GET request on a URL.
   *
   * @param url
   * @return
   * @throws Exception
   */
  public static String httpGet(String url) throws Exception {
    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "Mozilla/5.0");
    int responseCode = con.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      return response.toString();
    } else {
      throw new Exception("GET request failed on " + url);
    }
  }
}
