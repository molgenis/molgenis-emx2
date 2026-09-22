export function errorToMessage(
  error: unknown,
  fallBackMessage: string
): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "response" in error &&
    typeof error.response === "object" &&
    error.response !== null &&
    "_data" in error.response &&
    typeof error.response._data === "object" &&
    error.response._data !== null &&
    "errors" in error.response._data &&
    Array.isArray(error.response._data.errors) &&
    error.response._data.errors[0]?.message
  ) {
    return error.response._data.errors[0].message;
  }
  return fallBackMessage;
}
