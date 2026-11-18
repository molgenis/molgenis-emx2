import { describe, expect, test } from "vitest";
import type { H3Event } from "h3";
import { dropPageOriginHeaders } from "../../../server/middleware/dropPageOriginHeaders";

function eventWith(method: string, headers: Record<string, string>) {
  return {
    method,
    node: { req: { headers: { ...headers } } },
  } as unknown as H3Event;
}

const visitorHeaders = {
  host: "catalogue.example.org",
  origin: "https://www.google.com",
  referer: "https://www.google.com/",
  cookie: "session=abc",
};

describe("dropPageOriginHeaders", () => {
  test("page requests lose the visitor's origin and referer", () => {
    const event = eventWith("GET", visitorHeaders);
    dropPageOriginHeaders(event);
    expect(event.node.req.headers).toEqual({
      host: "catalogue.example.org",
      cookie: "session=abc",
    });
  });

  test("head requests lose the visitor's origin and referer", () => {
    const event = eventWith("HEAD", {
      referer: "android-app://com.slack/",
    });
    dropPageOriginHeaders(event);
    expect(event.node.req.headers).toEqual({});
  });

  test("state-changing requests keep origin and referer for the backend csrf check", () => {
    const event = eventWith("POST", visitorHeaders);
    dropPageOriginHeaders(event);
    expect(event.node.req.headers).toEqual(visitorHeaders);
  });
});
