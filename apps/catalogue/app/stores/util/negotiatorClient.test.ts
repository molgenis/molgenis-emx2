import { describe, expect, it } from "vitest";
import {
  getHumanReadableString,
  handleNegotiatorV3Error,
  toNegotiatorFormat,
} from "./negotiatorClient";
import type { IResources } from "~~/interfaces/catalogue";

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

describe("toNegotiatorFormat", () => {
  it("should convert datasets to the negotiator format", () => {
    const datasets = {
      dataset1: { pid: "pid1", name: "Dataset 1" } as IResources,
      dataset2: { pid: "pid2", name: "Dataset 2" } as IResources,
    };
    const result = toNegotiatorFormat(datasets);
    expect(result).toEqual([
      { id: "pid1", name: "Dataset 1" },
      { id: "pid2", name: "Dataset 2" },
    ]);
  });
});

describe("getHumanReadableString", () => {
  it("should convert datasets to a human readable string", () => {
    const datasets = {
      dataset1: { pid: "pid1", name: "Dataset 1" } as IResources,
      dataset2: { pid: "pid2", name: "Dataset 2" } as IResources,
    };
    const result = getHumanReadableString(datasets);
    expect(result).toEqual("Dataset 1 (pid1), Dataset 2 (pid2)");
  });
});
