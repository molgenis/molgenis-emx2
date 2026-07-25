import type { IMgError } from "~~/interfaces/types";

const hasApiErrorMessages = (value: unknown): boolean =>
  typeof value === "object" &&
  value !== null &&
  "errors" in value &&
  Array.isArray(value.errors) &&
  value.errors.every(
    (apiError) =>
      typeof apiError === "object" &&
      apiError !== null &&
      "message" in apiError &&
      typeof apiError.message === "string"
  );

export const isMgError = (value: unknown): value is IMgError =>
  typeof value === "object" &&
  value !== null &&
  "message" in value &&
  typeof value.message === "string" &&
  "statusCode" in value &&
  typeof value.statusCode === "number" &&
  "data" in value &&
  hasApiErrorMessages(value.data);

export const logError = (error: IMgError, contextMsg?: string) => {
  if (contextMsg) {
    console.log(`[ERROR] ${contextMsg}`);
  }

  console.log(`[ERROR] StatusCode: ${error.statusCode}`);
  console.log(`[ERROR] Message: ${error.message}`);
  if (error.data?.errors) {
    console.log("[ERROR] MESSAGES FROM API: ");
    error.data.errors.forEach((e: { message: string }, lineNr) =>
      console.log(`    ${lineNr}: ${e.message}`)
    );
  }
};
