import { describe, expect, test } from "vitest";
import { FetchError } from "ofetch";
import { fetchErrorToNuxtError } from "../../../app/utils/fetchErrorToNuxtError";

function fetchError(statusCode: number, backendMessage?: string): FetchError {
  const error = new FetchError("request failed");
  error.statusCode = statusCode;
  if (backendMessage) {
    error.response = {
      _data: { errors: [{ message: backendMessage }] },
    } as unknown as FetchError["response"];
  }
  return error;
}

describe("fetchErrorToNuxtError", () => {
  test("propagates the http status code of a FetchError", () => {
    const result = fetchErrorToNuxtError(fetchError(404), "fallback");
    expect(result.statusCode).toBe(404);
  });

  test("uses the backend error message when present", () => {
    const result = fetchErrorToNuxtError(
      fetchError(404, "Schema 'foo' unknown. Might you need to sign in?"),
      "fallback"
    );
    expect(result.message).toBe(
      "Schema 'foo' unknown. Might you need to sign in?"
    );
  });

  test("falls back to the given message without a backend message", () => {
    const result = fetchErrorToNuxtError(fetchError(400), "fallback message");
    expect(result.message).toBe("fallback message");
  });

  test("defaults to 500 for non-fetch errors", () => {
    const result = fetchErrorToNuxtError(new Error("boom"), "fallback");
    expect(result.statusCode).toBe(500);
    expect(result.message).toBe("fallback");
  });
});
