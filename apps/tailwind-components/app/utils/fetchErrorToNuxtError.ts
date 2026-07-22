import { createError } from "nuxt/app";
import { FetchError } from "ofetch";
import { errorToMessage } from "./errorToMessage";

export function fetchErrorToNuxtError(error: unknown, fallbackMessage: string) {
  return createError({
    statusCode: error instanceof FetchError ? (error.statusCode ?? 500) : 500,
    message: errorToMessage(error, fallbackMessage),
    cause: error,
  });
}
