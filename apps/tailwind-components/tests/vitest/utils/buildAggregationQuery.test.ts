import { describe, test, expect } from "vitest";
import { buildAggregationQuery } from "../../../app/utils/cms/buildAggregationQuery";

describe("cms: build aggregation query: ", () => {
  test("query for counts is built", () => {
    const expectedQuery =
      "query getAggregation($filter: PetFilter) {Pet_agg(filter: $filter) {count}}";
    const queryInfo = buildAggregationQuery({
      table: "Pet",
      method: "count",
    });
    const query = queryInfo.query.replaceAll(/\n\s+/g, "");
    expect(query).toEqual(expectedQuery);
    expect(queryInfo.gqlTableName).toEqual("Pet_agg");
    expect(queryInfo.gqlBodyIsNested).toBeFalsy();
  });

  test("query for _sum is built", () => {
    const expectedQuery =
      "query getAggregation($filter: PetFilter) {Pet_agg(filter: $filter) {_sum {weight}}}";
    const queryInfo = buildAggregationQuery({
      table: "Pet",
      column: "weight",
      method: "_sum",
    });
    const query = queryInfo.query.replaceAll(/\n\s+/g, "");
    expect(query).toEqual(expectedQuery);
    expect(queryInfo.gqlTableName).toEqual("Pet_agg");
    expect(queryInfo.gqlBodyIsNested).toBeTruthy();
  });
});
