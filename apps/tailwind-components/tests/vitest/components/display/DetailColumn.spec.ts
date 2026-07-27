import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import DetailColumn from "../../../../app/components/display/DetailColumn.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

const KEYWORD_ANCESTRY: Record<string, string | null> = {
  Diseases: null,
  "Diseases of the respiratory system (J00-J99)": "Diseases",
  "Lung function": "Diseases of the respiratory system (J00-J99)",
  Spirometry: "Lung function",
  FEF25: "Spirometry",
  "Absolute (FEF25)": "FEF25",
};

function keywordsColumn(ontologySchemaId: string): IColumn {
  return {
    id: "keywords",
    label: "Keywords",
    columnType: "ONTOLOGY_ARRAY",
    refTableId: "Keywords",
    refSchemaId: ontologySchemaId,
    position: 0,
  } as IColumn;
}

function stubOntologyGraphql(requestedTermsPerCall: string[][] = []) {
  vi.stubGlobal(
    "$fetch",
    vi.fn(
      async (
        _url: string,
        options: { body: { query: string; variables: any } }
      ) => {
        const requested: string[] =
          options.body.variables.filter._match_any_including_parents;
        requestedTermsPerCall.push(requested);
        const reached = new Set<string>();
        for (const term of requested) {
          let current: string | null = term;
          while (current && !reached.has(current)) {
            reached.add(current);
            current = KEYWORD_ANCESTRY[current] ?? null;
          }
        }
        return {
          data: {
            Keywords: [...reached].map((name) => ({
              name,
              label: name,
              parent: KEYWORD_ANCESTRY[name]
                ? { name: KEYWORD_ANCESTRY[name] }
                : null,
            })),
          },
        };
      }
    )
  );
}

async function mountKeywords(ontologySchemaId: string, termNames: string[]) {
  const wrapper = mount(DetailColumn, {
    props: {
      column: keywordsColumn(ontologySchemaId),
      value: termNames.map((name) => ({ name, label: name })),
      schemaId: "AnyDataSchema",
    },
  });
  await flushPromises();
  return wrapper;
}

function nodeNamesByDepth(
  list: Element,
  depth = 0,
  namesByDepth: string[][] = []
): string[][] {
  for (const item of Array.from(list.children)) {
    if (item.tagName !== "LI") continue;
    const label = item.querySelector("span.flex.justify-center");
    (namesByDepth[depth] ??= []).push(label?.textContent?.trim() ?? "");
    const sublist = Array.from(item.children).find(
      (child) => child.tagName === "UL"
    );
    if (sublist) nodeNamesByDepth(sublist, depth + 1, namesByDepth);
  }
  return namesByDepth;
}

function renderedTree(wrapper: {
  find: (selector: string) => any;
}): string[][] {
  return nodeNamesByDepth(wrapper.find("ul").element);
}

describe("display/DetailColumn.vue ontology ancestry", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("nests a six-level term under a single root, so no ancestor beyond the old four-hop fetch appears as a root of its own", async () => {
    stubOntologyGraphql();

    const wrapper = await mountKeywords("deepOntologySchema", [
      "Absolute (FEF25)",
    ]);

    expect(renderedTree(wrapper)).toEqual([
      ["Diseases"],
      ["Diseases of the respiratory system (J00-J99)"],
      ["Lung function"],
      ["Spirometry"],
      ["FEF25"],
      ["Absolute (FEF25)"],
    ]);
  });

  it("merges terms from different branches of the same ontology into one tree", async () => {
    stubOntologyGraphql();

    const wrapper = await mountKeywords("mergedOntologySchema", [
      "FEF25",
      "Lung function",
    ]);

    expect(renderedTree(wrapper)).toEqual([
      ["Diseases"],
      ["Diseases of the respiratory system (J00-J99)"],
      ["Lung function"],
      ["Spirometry"],
      ["FEF25"],
    ]);
  });

  it("renders a root-only ontology as a flat list rather than a tree", async () => {
    stubOntologyGraphql();

    const wrapper = await mountKeywords("flatOntologySchema", ["Diseases"]);

    expect(wrapper.text()).toContain("Diseases");
    expect(wrapper.find("ul").exists()).toBe(false);
  });

  it("asks the ontology table only for terms it has not already resolved", async () => {
    const requestedTermsPerCall: string[][] = [];
    stubOntologyGraphql(requestedTermsPerCall);

    await mountKeywords("cachedOntologySchema", ["FEF25"]);
    await mountKeywords("cachedOntologySchema", ["FEF25", "Absolute (FEF25)"]);

    expect(requestedTermsPerCall).toEqual([["FEF25"], ["Absolute (FEF25)"]]);
  });
});
