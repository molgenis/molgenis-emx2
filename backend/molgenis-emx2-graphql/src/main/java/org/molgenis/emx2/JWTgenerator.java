package org.molgenis.emx2;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.SecureRandom;
import java.util.Date;

public class JWTgenerator {
  private static byte[] sharedSecret;
  private static MACSigner signer;
  private static JWSVerifier verifier;

  private static void init() {
    try {
      // Generate random 256-bit (32-byte) shared secret
      sharedSecret = new byte[32];
      new SecureRandom().nextBytes(sharedSecret);

      // Create HMAC signer
      signer = new MACSigner(sharedSecret);
      verifier = new MACVerifier(sharedSecret);
    } catch (Exception kle) {
      throw new MolgenisException(kle.getMessage());
    }
  }

  // create token
  public static String createTokenForUser(String user, int minutes) {
    try {
      if (signer == null) {
        init();
      }
      // Prepare JWT with claims set
      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .subject(user)
              .expirationTime(new Date(new Date().getTime() + minutes * 1000))
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
  public static String getUserFromToken(String token) {
    // On the consumer side, parse the JWS and verify its HMAC
    try {
      if (signer == null) {
        init();
      }
      SignedJWT signedJWT = SignedJWT.parse(token);
      if (signedJWT.verify(verifier)
          && new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime())) {
        return signedJWT.getJWTClaimsSet().getSubject();
      } else {
        throw new MolgenisException("Invalid token or token expired");
      }
    } catch (Exception e) {
      throw new MolgenisException("Cannot parse token");
    }
  }
}
