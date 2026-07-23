import { afterEach, describe, expect, it, vi } from "vitest";
import {
  doNegotiatorV3Request,
  handleNegotiatorV3Error,
} from "./negotiatorClient";
import type { IResources, IVariables } from "~~/interfaces/catalogue";
import type { ICartItem } from "~~/interfaces/types";

describe("handleNegotiatorV3Error", () => {
  it("should return a 400 error with the details message", async () => {
    const response = new Response(JSON.stringify({ detail: "Invalid input" }), {
      status: 400,
    });
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "Negotiator responded with code 400, invalid input. Detail: Invalid input"
    );
  });

  it("should return a 401 error with the details message", async () => {
    const response = new Response(
      JSON.stringify({ detail: "Not authorised" }),
      {
        status: 401,
      }
    );
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "Negotiator responded with code 401, not authorised. Detail: Not authorised"
    );
  });

  it("should return a 404 error with the details message", async () => {
    const response = new Response(JSON.stringify({ detail: "Not found" }), {
      status: 404,
    });
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "Negotiator not found, error code 404. Detail: Not found"
    );
  });

  it("should return a 413 error with the details message", async () => {
    const response = new Response(
      JSON.stringify({ detail: "Request too large" }),
      {
        status: 413,
      }
    );
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "Negotiator responded with code 413, request too large. Detail: Request too large"
    );
  });

  it("should return a 500 error with the details message", async () => {
    const response = new Response(
      JSON.stringify({ detail: "Internal server error" }),
      {
        status: 500,
      }
    );
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "Negotiator responded with code 500, internal server error. Detail: Internal server error"
    );
  });

  it("should return an unknown error message with the details message", async () => {
    const response = new Response(JSON.stringify({ detail: "Unknown error" }), {
      status: 501,
    });
    const result = await handleNegotiatorV3Error(response);
    expect(result).toEqual(
      "An unknown error occurred with the Negotiator. Please try again later. Detail: Unknown error"
    );
  });
});

describe("doNegotiatorV3Request", () => {
  const resourceItem: ICartItem = {
    id: "resource:res1",
    label: "res1",
    type: "resource",
    pid: "pid1",
    name: "Dataset 1",
  };
  const variableItem: ICartItem = {
    id: "variable:network1:cohort1:core:height",
    label: "network1: core.height",
    type: "variable",
  };

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("should post only resources, with variables in the human readable text", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}"));
    vi.stubGlobal("fetch", fetchMock);

    await doNegotiatorV3Request(
      [resourceItem, variableItem],
      "https://negotiator.example/api"
    );

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, options] = fetchMock.mock.calls[0]!;
    expect(url).toEqual("https://negotiator.example/api");
    expect(options.method).toEqual("POST");

    const body = JSON.parse(options.body);
    expect(body.url).toEqual(window.location.origin);
    expect(body.humanReadable).toEqual(
      "Resources: Dataset 1 (pid1); Variables: network1: core.height"
    );
    expect(body.resources).toEqual([{ id: "pid1", name: "Dataset 1" }]);
  });

  it("should not produce resources for a variables-only cart", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}"));
    vi.stubGlobal("fetch", fetchMock);

    await doNegotiatorV3Request(
      [variableItem],
      "https://negotiator.example/api"
    );

    const body = JSON.parse(fetchMock.mock.calls[0]![1].body);
    expect(body.resources).toEqual([]);
    expect(body.humanReadable).toEqual("Variables: network1: core.height");
  });
});
