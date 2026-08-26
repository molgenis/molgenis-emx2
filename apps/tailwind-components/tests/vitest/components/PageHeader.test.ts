import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import PageHeader from "../../../app/components/PageHeader.vue";
import ContentClamp from "../../../app/components/ContentClamp.vue";

const pageHeaderSimple = mount(PageHeader, {
  props: {
    title: "My Title",
    description: "My description",
  },
});

test("PageHeader should contain truncation component by default (for description)", async () => {
  expect(pageHeaderSimple.findComponent(ContentClamp).exists()).toBe(true);
  expect(pageHeaderSimple.findComponent(ContentClamp).props("maxLines")).toBe(
    5
  );
});
