import { FetchError } from "ofetch";
import { errorToMessage } from "./errorToMessage";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly statusCode: number,
    options?: { cause?: unknown }
  ) {
    super(message, options);
    this.name = "ApiError";
  }
}

export function toApiError(error: unknown, fallbackMessage: string): ApiError {
  return new ApiError(
    errorToMessage(error, fallbackMessage),
    error instanceof FetchError ? (error.statusCode ?? 500) : 500,
    { cause: error }
  );
}
