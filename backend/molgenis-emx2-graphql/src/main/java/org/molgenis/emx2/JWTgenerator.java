package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import org.molgenis.emx2.utils.EnvironmentProperty;

public class JWTgenerator {
  private static byte[] sharedSecret;
  private static MACSigner signer;
  private static JWSVerifier verifier;

  private static void init() {
    try {
      sharedSecret = null;
      String key =
          (String)
              EnvironmentProperty.getParameter(Constants.MOLGENIS_JWT_SHARED_SECRET, null, STRING);
      if (key != null) {
        // try to parse from environment variable
        sharedSecret = key.getBytes();
      } else {
        // Generate random 256-bit (32-byte) shared secret
        sharedSecret = new byte[32];
        new SecureRandom().nextBytes(sharedSecret);
      }

      // check enough bytes
      if (sharedSecret.length < 32) {
        throw new MolgenisException(
            Constants.MOLGENIS_JWT_SHARED_SECRET
                + " was not secure enough, should be 32 bytes/256bit");
      }

      // Create HMAC signer
      signer = new MACSigner(sharedSecret);
      verifier = new MACVerifier(sharedSecret);
    } catch (Exception kle) {
      throw new MolgenisException(kle.getMessage());
    }
  }

  /*Token should either have id */
  public static String createNamedTokenForUser(Database db, String user, String tokenId) {
    Calendar c = Calendar.getInstance();
    c.setTime(new Date());
    // no enddate for these tokens (100 years), but lets put a century so code checking date does
    // work
    // future enhancement is to make duration configurable
    c.add(Calendar.YEAR, 100);
    String token = createNamedTokenForUser(db, user, tokenId, c.getTime());
    // add token to user settings
    db.getUser(user).addToken(tokenId);
    return token;
  }

  /* Temporary token has an expiration time */
  public static String createTemporaryToken(Database db, String user) {
    return createNamedTokenForUser(
        // half our in future, is 30 * 60 * 1000 milliseconds
        db, user, "temporary", Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
  }

  private static String createNamedTokenForUser(
      Database db, String user, String tokenId, Date expirationTime) {
    requireNonNull(user);
    requireNonNull(tokenId);
    requireNonNull(expirationTime);
    if (!db.getActiveUser().equals(ADMIN_USER) && !db.getActiveUser().equals(user)) {
      throw new MolgenisException("Cannot create token for other users unless admin");
    }
    try {
      if (signer == null) {
        init();
      }
      // Prepare JWT with claims set
      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(user)
              .jwtID(tokenId)
              .issueTime(new Date())
              .expirationTime(expirationTime)
              .build();

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

      // Apply the HMAC protection
      signedJWT.sign(signer);

      // Serialize to compact form, produces something like
      // eyJhbGciOiJIUzI1NiJ9.SGVsbG8sIHdvcmxkIQ.onO9Ihudz3WkiauDO2Uhyuz0Y18UASXlSc1eS0NkWyA
      return signedJWT.serialize();
    } catch (JOSEException je) {
      throw new MolgenisException(je.getMessage());
    }
  }

  // parse token
  public static String getUserFromToken(Database database, String token) {
    // On the consumer side, parse the JWS and verify its HMAC
    try {
      if (signer == null) {
        init();
      }
      SignedJWT signedJWT = SignedJWT.parse(token);
      Date experationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      String tokenId = signedJWT.getJWTClaimsSet().getJWTID();
      String userName = signedJWT.getJWTClaimsSet().getSubject();
      if (signedJWT.verify(verifier) && new Date().before(experationTime)) {
        // verify user is known
        User user = database.getUser(userName);
        // if temp token we must verify has experationTime not too far in the future
        if (user != null && "temporary".equals(tokenId)) {
          Instant thirtyMinutes = Instant.now().plus(30, ChronoUnit.MINUTES);
          if (experationTime.before(Date.from(thirtyMinutes))) {
            return user.getUsername();
          }
        }
        // else if not temporary we must verify it is known
        else if (user != null && user.hasToken(tokenId)) {
          return user.getUsername();
        }
      }
    } catch (Exception e) {
      throw new MolgenisException("Cannot parse token");
    }
    // else we throw exception
    throw new MolgenisException("Invalid token or token expired");
  }
}
