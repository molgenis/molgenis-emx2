package org.molgenis.emx2.sql;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.User;
import org.molgenis.emx2.utils.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JWTgenerator {
  // 32 bytes / 256 bit, the minimum for HS256
  public static final int SHARED_SECRET_LENGTH = 32;
  private static final Logger logger = LoggerFactory.getLogger(JWTgenerator.class);

  private static byte[] sharedSecret;
  private static MACSigner signer;
  private static JWSVerifier verifier;

  /** Generates a shared secret strong enough to sign tokens with (see {@link #init}). */
  public static String generateSharedSecret() {
    return new RandomString(SHARED_SECRET_LENGTH).nextString();
  }

  private static void init(Database database) {
    try {
      Objects.requireNonNull(database);
      sharedSecret = database.resolveJwtSharedSecret().getBytes();
      // check enough bytes
      if (sharedSecret.length < SHARED_SECRET_LENGTH) {
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

  public static String createTemporaryToken(Database db) {
    return createTemporaryToken(db, db.getActiveUser());
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
        init(db);
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
    if (signer == null) {
      init(database);
    }

    final SignedJWT signedJWT;
    try {
      signedJWT = SignedJWT.parse(token);
    } catch (ParseException e) {
      logger.warn("JWT parsing failed. tokenLength={}", token != null ? token.length() : 0, e);
      throw new MolgenisException("Cannot parse token", e);
    }

    final JWTClaimsSet claimsSet;
    try {
      claimsSet = signedJWT.getJWTClaimsSet();
    } catch (ParseException e) {
      logger.warn("Failed to get JWT claims.", e);
      throw new MolgenisException("Cannot parse token", e);
    }

    final Date expirationTime = claimsSet.getExpirationTime();
    final String tokenId = claimsSet.getJWTID();
    final String userName = claimsSet.getSubject();
    if (expirationTime == null || tokenId == null || userName == null) {
      throw new MolgenisException("Invalid token or token expired");
    }

    final boolean isVerified;
    try {
      isVerified = signedJWT.verify(verifier);
    } catch (JOSEException je) {
      logger.warn("JWT verification failed. jwtId={}", tokenId, je);
      throw new MolgenisException("Cannot verify token", je);
    }

    if (!isVerified || !new Date().before(expirationTime)) {
      throw new MolgenisException("Invalid token or token expired");
    }

    final User user = database.getUser(userName);
    if (user == null) {
      throw new MolgenisException("Invalid token or token expired");
    }

    if ("temporary".equals(tokenId)) {
      Instant thirtyMinutes = Instant.now().plus(30, ChronoUnit.MINUTES);
      if (expirationTime.before(Date.from(thirtyMinutes))) {
        return user.getUsername();
      }
    } else if (user.hasToken(tokenId)) {
      return user.getUsername();
    }

    throw new MolgenisException("Invalid token or token expired");
  }
}
