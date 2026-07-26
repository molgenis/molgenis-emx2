import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import DetailView from "../../../../app/components/display/DetailView.vue";
import type { IColumn } from "../../../../../metadata-utils/src/types";

function birdsColumns(): IColumn[] {
  return [
    {
      id: "overview",
      label: "Overview",
      columnType: "SECTION",
      section: "overview",
    },
    { id: "name", label: "Name", columnType: "STRING", section: "overview" },
    {
      id: "details",
      label: "Details",
      columnType: "HEADING",
      section: "overview",
    },
    { id: "age", label: "Age", columnType: "INT", heading: "details" },
    { id: "sightings", label: "Sightings", columnType: "HEADING" },
    {
      id: "region",
      label: "Region",
      columnType: "STRING",
      heading: "sightings",
    },
    { id: "ring", label: "Ring", columnType: "STRING" },
  ];
}

const birdData = {
  name: "Tweety",
  age: 3,
  region: "Groningen",
  ring: "NL-1234",
};

function mountDetailView() {
  return mount(DetailView, {
    props: { columns: birdsColumns(), data: birdData, showSideNav: false },
  });
}

describe("display/DetailView.vue section grouping", () => {
  it("groups the columns under their own SECTION and HEADING, with the columns belonging to neither first", () => {
    const wrapper = mountDetailView();

    const groups = wrapper.findAll("section").map((section) => ({
      heading: section.find("h2").exists() ? section.find("h2").text() : null,
      terms: section.findAll("dt").map((term) => term.text()),
    }));

    expect(groups).toEqual([
      { heading: null, terms: ["Ring"] },
      { heading: "Overview", terms: ["Name"] },
      { heading: "Details", terms: ["Age"] },
      { heading: "Sightings", terms: ["Region"] },
    ]);
  });
});
