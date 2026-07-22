import { createError } from "nuxt/app";
import { FetchError } from "ofetch";
import { errorToMessage } from "./errorToMessage";

export function fetchErrorToNuxtError(error: unknown, fallbackMessage: string) {
  const statusCode = error instanceof FetchError ? error.statusCode : undefined;
  return createError({
    statusCode: statusCode ?? 500,
    message: errorToMessage(error, fallbackMessage),
    cause: error,
  });
}
