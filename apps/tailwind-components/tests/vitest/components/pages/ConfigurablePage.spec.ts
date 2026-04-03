import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import ConfigurablePage from "../../../../app/components/pages/ConfigurablePage.vue";
import type { IConfigurablePages } from "../../../../types/cms";

const pageData: IConfigurablePages = {
  mg_tableclass: "cms.Configurable pages",
  name: "Home",
  blockOrder: [
    {
      id: "page-banner-order",
      order: 0,
      block: {
        mg_tableclass: "cms.Headers",
        id: "home-banner",
        title: "My app title",
        subtitle: "A longer description about my app",
        backgroundImage: "path/to/some/image",
        titleIsCentered: true,
      },
    },
    {
      id: "welcome-section-order",
      order: 1,
      block: {
        mg_tableclass: "cms.Sections",
        id: "welcome-section",
        enableFullScreenWidth: true,
        componentOrder: [
          {
            id: "welcome-section-heading-order",
            order: 0,
            component: {
              mg_tableclass: "cms.Headings",
              id: "welcome-section-heading",
              text: "Welcome",
            },
          },
          {
            id: "welcome-section-paragraph",
            order: 0,
            component: {
              mg_tableclass: "cms.Paragraphs",
              id: "welcome-section-paragraph",
              text: "Dolore reprehenderit quis culpa adipisicing in elit Lorem nostrud laborum in velit.",
            },
          },
        ],
      },
    },
  ],
};

const page = mount(ConfigurablePage, {
  props: {
    content: pageData,
  },
});

describe("Configurable page", () => {
  test("Blocks are rendered in the correct order", () => {
    const firstElem = page.find("*:first-child");
    const secondElem = page.find("*:nth-child(2)");
    expect(firstElem.html()).toContain('<header id="home-banner"');
    expect(secondElem.html()).toContain('<div id="welcome-section"');
  });
});
