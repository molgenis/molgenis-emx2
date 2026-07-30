import { describe, it, expect } from "vitest";
import { createRouter, createMemoryHistory } from "vue-router";
import type { LocationQuery } from "vue-router";

import { landingPageRedirect } from "./landingPageRedirect";

function resolveRedirectPath(query: LocationQuery, cohortOnlyConfig: boolean) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: "/all", component: { template: "<div />" } }],
  });
  const target = landingPageRedirect(query, cohortOnlyConfig);
  return target === null ? null : router.resolve(target).fullPath;
}

describe("landingPageRedirect", () => {
  it("does not redirect when neither the query flag nor the runtime config asks for cohort-only mode", () => {
    expect(landingPageRedirect({}, false)).toEqual(null);
  });

  it("does not redirect when the query flag is present but set to false", () => {
    expect(landingPageRedirect({ "cohort-only": "false" }, false)).toEqual(
      null
    );
  });

  it("redirects to a bare /all with no query when the runtime config asks for cohort-only mode", () => {
    expect(landingPageRedirect({}, true)).toEqual({
      path: "/all",
      query: {},
    });
  });

  it("forwards theme, logo and cohort-only through the redirect", () => {
    expect(
      landingPageRedirect(
        {
          theme: "uncan-connect",
          logo: "uncan-white.png",
          "cohort-only": "true",
        },
        false
      )
    ).toEqual({
      path: "/all",
      query: {
        theme: "uncan-connect",
        logo: "uncan-white.png",
        "cohort-only": "true",
      },
    });
  });

  it("forwards a query param it knows nothing about", () => {
    expect(landingPageRedirect({ "some-future-param": "value" }, true)).toEqual(
      {
        path: "/all",
        query: { "some-future-param": "value" },
      }
    );
  });

  it("resolves to /all with no question mark when there is no query to forward", () => {
    expect(resolveRedirectPath({}, true)).toEqual("/all");
  });

  it("resolves to /all carrying every forwarded param", () => {
    expect(
      resolveRedirectPath(
        {
          theme: "uncan-connect",
          logo: "uncan-white.png",
          "cohort-only": "true",
        },
        false
      )
    ).toEqual("/all?theme=uncan-connect&logo=uncan-white.png&cohort-only=true");
  });
});
