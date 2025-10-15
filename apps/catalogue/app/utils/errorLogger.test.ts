import { afterAll, describe, it, expect, vi } from "vitest";
import { logError } from "./errorLogger";

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
