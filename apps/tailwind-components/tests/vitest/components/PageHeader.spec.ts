import { mount } from "@vue/test-utils";
import { expect, test } from "vitest";
import PageHeader from "../../../app/components/PageHeader.vue";
import ContentReadMore from "../../../app/components/ContentReadMore.vue";

const pageHeaderSimple = mount(PageHeader, {
  props: {
    title: "My Title",
    description: "My description",
  },
});

test("PageHeader should contain truncation component by default (for description)", async () => {
  expect(pageHeaderSimple.findComponent(ContentReadMore).exists()).toBe(true);
});
