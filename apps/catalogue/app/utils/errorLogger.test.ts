import { afterAll, describe, it, expect, vi } from "vitest";
import { createFetch, FetchError } from "ofetch";
import { isMgError, logError } from "./errorLogger";

describe("logError", () => {
  const consoleMock = vi
    .spyOn(console, "log")
    .mockImplementation(() => undefined);

  afterAll(() => {
    consoleMock.mockReset();
  });

  const error = {
    message: "a message to you ",
    statusCode: 418,
    data: { errors: [{ message: "test" }] },
  };

  it("should log the error", () => {
    logError(error);
    expect(consoleMock).toBeCalledWith("[ERROR] MESSAGES FROM API: ");
    expect(consoleMock).toBeCalledWith("[ERROR] StatusCode: 418");
    expect(consoleMock).toBeCalledWith("[ERROR] Message: a message to you ");
    expect(consoleMock).toBeCalledWith("    0: test");
  });

  it("should log the error context", () => {
    logError(error, "test context");
    expect(consoleMock).nthCalledWith(5, "[ERROR] test context");
  });
});

describe("isMgError", () => {
  const failingApiCall = () => {
    const fetchGraphql = createFetch({
      fetch: async () =>
        new Response(JSON.stringify({ errors: [{ message: "test" }] }), {
          status: 418,
          headers: { "content-type": "application/json" },
        }),
      Headers,
    });
    return fetchGraphql("/schema/graphql", { method: "POST" }).catch(
      (error) => error
    );
  };

  it("should accept the fetch error a failing api call throws", async () => {
    const error = await failingApiCall();
    expect(error).toBeInstanceOf(FetchError);
    expect(isMgError(error)).toBe(true);
  });

  it("should reject the response body inside that fetch error", async () => {
    const error = await failingApiCall();
    expect(error.data).toEqual({ errors: [{ message: "test" }] });
    expect(isMgError(error.data)).toBe(false);
  });

  it("should accept a payload carrying every field logError reads", () => {
    expect(
      isMgError({
        message: "a message to you ",
        statusCode: 418,
        data: { errors: [{ message: "test" }] },
      })
    ).toBe(true);
  });

  it.each([
    [
      "a nuxt error page body, which carries no api errors",
      {
        url: "/all",
        statusCode: 500,
        statusMessage: "",
        message: "internal server error",
        stack: "",
      },
    ],
    [
      "api errors that are not a list",
      {
        message: "a message to you ",
        statusCode: 418,
        data: { errors: { message: "test" } },
      },
    ],
    [
      "api errors without a message",
      {
        message: "a message to you ",
        statusCode: 418,
        data: { errors: [{ detail: "test" }] },
      },
    ],
    [
      "a status code sent as text",
      {
        message: "a message to you ",
        statusCode: "418",
        data: { errors: [{ message: "test" }] },
      },
    ],
    [
      "a message sent as a number",
      {
        message: 418,
        statusCode: 418,
        data: { errors: [{ message: "test" }] },
      },
    ],
    [
      "a body carrying no message at all",
      {
        statusCode: 418,
        data: { errors: [{ message: "test" }] },
      },
    ],
    ["a plain string body", "Internal Server Error"],
    ["no body at all", undefined],
    ["a null body", null],
  ])("should reject %s", (_description, payload) => {
    expect(isMgError(payload)).toBe(false);
  });
});
