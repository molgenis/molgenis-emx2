import type { LocationQuery } from "vue-router";

export function landingPageRedirect(
  query: LocationQuery,
  cohortOnlyConfig: boolean
) {
  const cohortOnly = query["cohort-only"] === "true" || cohortOnlyConfig;
  if (!cohortOnly) {
    return null;
  }
  return { path: "/all", query };
}
