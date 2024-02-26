import type { IMgError } from "~~/interfaces/types";

export const logError = (error: IMgError, contextMsg?: string) => {
  if (contextMsg) {
    console.log(`[ERROR] ${contextMsg}`);
  }

  console.log(`[ERROR] StatusCode: ${error.statusCode}`);
  console.log(`[ERROR] Message: ${error.message}`);
  if (error.data.errors) {
    console.log("[ERROR] MESSAGES FROM API: ");
    error.data.errors.forEach((e: { message: string }, lineNr) =>
      console.log(`    ${lineNr}: ${e.message}`)
    );
  }
};
