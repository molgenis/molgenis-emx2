import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import ShowHide from "./ShowHide.vue";

async function mountOpenedShowHide() {
  const wrapper = mount(ShowHide, {
    props: {
      label: "columns",
      icon: "columns",
      checkAttribute: "showColumn",
      columns: [
        { id: "overview", label: "Overview", columnType: "SECTION" },
        { id: "details", label: "Details", columnType: "HEADING" },
        { id: "name", label: "Name", columnType: "STRING" },
      ],
    },
  });
  await wrapper.find("button").trigger("click");
  return wrapper;
}

describe("ShowHide", () => {
  test("it should offer a checkbox for the data columns only, not for the layout columns", async () => {
    const wrapper = await mountOpenedShowHide();

    const labels = wrapper
      .findAll(".form-check label")
      .map((label) => label.text());

    expect(labels).to.deep.equal(["Name"]);
  });
});
