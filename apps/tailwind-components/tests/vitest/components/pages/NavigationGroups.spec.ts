import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import NavigationGroups from "../../../../app/components/pages/Navigation/NavigationGroups.vue";

const wrapper = mount(NavigationGroups, {
  props: {
    id: "vitest-navigation-group",
    links: [
      {
        id: "EFDMXMMRhW91",
        title: "Learn more",
        description:
          "Visit our docs to learn more about our software and how to use it",
        url: "https://molgenis.github.io/molgenis-emx2/#/",
        urlLabel: "Read the docs",
        urlIsExternal: true,
      },
      {
        id: "2UoF7nu62PnR",
        title: "Contribute to the code",
        description:
          "Checkout our GitHub repository and contribute to our codebase",
        url: "https://github.com/molgenis/molgenis-emx2",
        urlLabel: "Go to github",
        urlIsExternal: true,
      },
    ],
  },
});

describe("Configurable Pages: NavigationGroup", () => {
  test("rendered", () => {
    const cards = wrapper.findAll("nav ul li div");
    expect(cards.length).toEqual(2);
    cards.forEach((card) => {
      expect(card.findAll("h3").length).toEqual(1);
      expect(card.findAll("p").length).toEqual(1);
      expect(card.findAll("a").length).toEqual(1);
      expect(card.findAll("svg").length).toEqual(1);
    });
  });
});
