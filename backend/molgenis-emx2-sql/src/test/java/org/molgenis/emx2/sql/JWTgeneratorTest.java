package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;

class JWTgeneratorTest {

  private Database db;
  private static final String TEST_USER = "testuser";
  private static final String OTHER_USER = "otheruser";
  private static final String ADMIN_USER = "admin";

  @BeforeEach
  void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.addUser(TEST_USER);
    db.addUser(OTHER_USER);
  }

  @Test
  void testGenerateSharedSecret() {
    String secret = JWTgenerator.generateSharedSecret();
    assertNotNull(secret);
    assertFalse(secret.isEmpty());
    // Should be 32 bytes
    assertEquals(JWTgenerator.SHARED_SECRET_LENGTH, secret.getBytes().length);
  }

  @Test
  void testGenerateSharedSecretUnique() {
    String secret1 = JWTgenerator.generateSharedSecret();
    String secret2 = JWTgenerator.generateSharedSecret();
    assertNotEquals(secret1, secret2);
  }

  @Test
  void testCreateTemporaryToken() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    assertNotNull(token);
    assertFalse(token.isEmpty());
    // JWT should have 3 parts separated by dots
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);
  }

  @Test
  void testCreateTemporaryTokenWithActiveUser() {
    db.setActiveUser(TEST_USER);
    String token = JWTgenerator.createTemporaryToken(db);
    assertNotNull(token);
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testCreateNamedTokenForUser() {
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "mytoken");
    assertNotNull(token);
    assertFalse(token.isEmpty());
    String[] parts = token.split("\\.");
    assertEquals(3, parts.length);
  }

  @Test
  void testGetUserFromTemporaryToken() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testGetUserFromNamedToken() {
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "mytoken");
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testGetUserFromTokenMultipleTokens() {
    String token1 = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "token1");
    String token2 = JWTgenerator.createNamedTokenForUser(db, OTHER_USER, "token2");

    String user1 = JWTgenerator.getUserFromToken(db, token1);
    String user2 = JWTgenerator.getUserFromToken(db, token2);

    assertEquals(TEST_USER, user1);
    assertEquals(OTHER_USER, user2);
  }

  @Test
  void testInvalidTokenThrowsException() {
    String invalidToken = "invalid.token.here";
    assertThrows(MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, invalidToken));
  }

  @Test
  void testEmptyTokenThrowsException() {
    assertThrows(MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, ""));
  }

  @Test
  void testNullTokenThrowsException() {
    assertThrows(NullPointerException.class, () -> JWTgenerator.getUserFromToken(db, null));
  }

  @Test
  void testModifiedTokenThrowsException() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    // Modify the token
    String modifiedToken = token.substring(0, token.length() - 5) + "XXXXX";
    assertThrows(MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, modifiedToken));
  }

  @Test
  void testAdminCanCreateTokenForOtherUser() {
    db.setActiveUser(ADMIN_USER);
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "admintoken");
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testNonAdminCannotCreateTokenForOtherUser() {
    db.setActiveUser(TEST_USER);
    assertThrows(
        MolgenisException.class,
        () -> JWTgenerator.createNamedTokenForUser(db, OTHER_USER, "unauthorized"));
  }

  @Test
  void testNonAdminCanCreateOwnToken() {
    db.setActiveUser(TEST_USER);
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "owntoken");
    assertNotNull(token);
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testTokenWithNullUser() {
    assertThrows(
        NullPointerException.class, () -> JWTgenerator.createNamedTokenForUser(db, null, "token"));
  }

  @Test
  void testTokenWithNullTokenId() {
    assertThrows(
        NullPointerException.class,
        () -> JWTgenerator.createNamedTokenForUser(db, TEST_USER, null));
  }

  @Test
  void testMultipleTemporaryTokensForSameUser() throws InterruptedException {
    String token1 = JWTgenerator.createTemporaryToken(db, TEST_USER);
    // Wait a small amount of time to ensure tokens have different issue times
    Thread.sleep(100);
    String token2 = JWTgenerator.createTemporaryToken(db, TEST_USER);

    // Both tokens should be valid and extract the same user
    assertEquals(TEST_USER, JWTgenerator.getUserFromToken(db, token1));
    assertEquals(TEST_USER, JWTgenerator.getUserFromToken(db, token2));
  }

  @Test
  void testNamedTokenPersistence() {
    String tokenId = "persistenttoken";
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, tokenId);

    // Verify the token is registered on the user
    assertTrue(db.getUser(TEST_USER).hasToken(tokenId));

    // Verify we can extract the user from the token
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testDifferentTokenIdsForSameUser() {
    String token1 = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "token1");
    String token2 = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "token2");

    assertNotEquals(token1, token2);
    assertEquals(TEST_USER, JWTgenerator.getUserFromToken(db, token1));
    assertEquals(TEST_USER, JWTgenerator.getUserFromToken(db, token2));
  }

  @Test
  void testTemporaryTokenHasExpirationTime() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    // Should not throw an exception if the token is still valid
    String user = JWTgenerator.getUserFromToken(db, token);
    assertEquals(TEST_USER, user);
  }

  @Test
  void testTokenForNonExistentUserThrowsException() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    db.removeUser(TEST_USER);
    assertThrows(MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, token));
  }

  @Test
  void testCreateTemporaryTokenReturnsValidJWT() {
    String token = JWTgenerator.createTemporaryToken(db, TEST_USER);
    // Token should be a valid JWT format
    assertTrue(token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));
  }

  @Test
  void testCreateNamedTokenReturnsValidJWT() {
    String token = JWTgenerator.createNamedTokenForUser(db, TEST_USER, "test");
    // Token should be a valid JWT format
    assertTrue(token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));
  }

  @Test
  void testMalformedJWTClaimsThrowsException() {
    // Create a JWT with a malformed claims payload (invalid base64 in the middle part)
    String malformedToken =
        "eyJhbGciOiJIUzI1NiJ9.invalid-payload-here.onO9Ihudz3WkiauDO2Uhyuz0Y18UASXlSc1eS0NkWyA";
    // Should throw MolgenisException when trying to parse the claims
    assertThrows(MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, malformedToken));
  }

  @Test
  void testInvalidSignatureThrowsException() {
    // Create a valid token
    String validToken = JWTgenerator.createTemporaryToken(db, TEST_USER);

    // Corrupt the signature (last part of the JWT)
    String[] parts = validToken.split("\\.");
    assertEquals(3, parts.length, "Valid JWT should have 3 parts");

    // Modify the signature part by changing a character
    String corruptedSignature = parts[2].substring(0, parts[2].length() - 1) + "X";
    String corruptedToken = parts[0] + "." + parts[1] + "." + corruptedSignature;

    // Should throw MolgenisException due to invalid signature
    // The verify will fail, leading to "Invalid token or token expired" exception
    MolgenisException exception =
        assertThrows(
            MolgenisException.class, () -> JWTgenerator.getUserFromToken(db, corruptedToken));
    assertTrue(
        exception.getMessage().contains("Invalid token")
            || exception.getMessage().contains("expired"),
        "Exception message should indicate invalid token or expiration");
  }

  @Test
  void testTemporaryTokenExpiringTooFarInFutureThrowsException() throws Exception {
    // Signed token, known user, and not-expired token, but temporary token expiry is > 30 minutes.
    JWTClaimsSet claimsSet =
        new JWTClaimsSet.Builder()
            .subject(TEST_USER)
            .jwtID("temporary")
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plus(31, ChronoUnit.MINUTES)))
            .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(new MACSigner(db.resolveJwtSharedSecret().getBytes()));

    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> JWTgenerator.getUserFromToken(db, signedJWT.serialize()));
    assertEquals("Invalid token or token expired", exception.getMessage());
  }

  @Test
  void testGetUserFromTokenInitializesSignerAndRejectsMissingClaims() throws Exception {
    Field signerField = JWTgenerator.class.getDeclaredField("signer");
    signerField.setAccessible(true);
    Object originalSigner = signerField.get(null);

    try {
      signerField.set(null, null);

      JWTClaimsSet claimsSet =
          new JWTClaimsSet.Builder()
              .jwtID("temporary")
              .issueTime(new Date())
              .expirationTime(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)))
              .build();

      SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
      signedJWT.sign(new MACSigner(db.resolveJwtSharedSecret().getBytes()));

      MolgenisException exception =
          assertThrows(
              MolgenisException.class,
              () -> JWTgenerator.getUserFromToken(db, signedJWT.serialize()));
      assertEquals("Invalid token or token expired", exception.getMessage());
    } finally {
      signerField.set(null, originalSigner);
    }
  }
}
