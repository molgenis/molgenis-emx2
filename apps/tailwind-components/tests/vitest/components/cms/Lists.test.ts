import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import UnorderedList from "../../../../app/components/cms/lists/UnorderedList.vue";
import OrderedList from "../../../../app/components/cms/lists/OrderedList.vue";

const testItems = [
  "This item has generic text",
  "Let's write some code: <code>const doSomething = (value:string) => console.log(value)</code>",
  "<em>This is text is italicised</em>",
  "This item is a hyperlink: <a href='https://github.com/molgenis/molgenis-emx2/>github.com/molgenis/molgenis-emx2</a>",
];

const unorderedListWrapper = mount(UnorderedList, {
  props: {
    id: "unordered-list-test",
    unorderedItems: testItems,
  },
});

const orderedListWrapper = mount(OrderedList, {
  props: { id: "ordered-list-test", orderedItems: testItems },
});

describe("Cms:Lists:", () => {
  test("Unordered list items render with html", async () => {
    expect(unorderedListWrapper.vm.$el.tagName).toBe("UL");
    expect(unorderedListWrapper.findAll("li").length).toBe(4);
    expect(unorderedListWrapper.find("li").text()).toBe(testItems[0]);
    expect(unorderedListWrapper.find("li a")).toBeTruthy();
    expect(unorderedListWrapper.find("li code")).toBeTruthy();
    expect(unorderedListWrapper.find("li em")).toBeTruthy();
  });

  test("Ordered list items render with html", async () => {
    expect(orderedListWrapper.vm.$el.tagName).toBe("OL");
    expect(orderedListWrapper.findAll("li").length).toBe(4);
    expect(orderedListWrapper.find("li").text()).toBe(testItems[0]);
    expect(orderedListWrapper.find("li a")).toBeTruthy();
    expect(orderedListWrapper.find("li code")).toBeTruthy();
    expect(orderedListWrapper.find("li em")).toBeTruthy();
  });
});
