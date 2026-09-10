import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import PageHeader from "../../../app/components/PageHeader.vue";
import ShowMore from "../../../app/components/ShowMore.vue";

const pageHeaderSimple = mount(PageHeader, {
  props: {
    title: "My Title",
    description: "My description",
  },
});

test("PageHeader bounds its description, on the house default rather than its own", async () => {
  expect(pageHeaderSimple.findComponent(ShowMore).exists()).toBe(true);
  expect(pageHeaderSimple.findComponent(ShowMore).props("maxLines")).toBe(3);
});
