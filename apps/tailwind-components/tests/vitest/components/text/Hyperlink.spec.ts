import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import Hyperlink from "../../../../app/components/text/Hyperlink.vue";
import BaseIcon from "../../../../app/components/BaseIcon.vue";

describe("Hyperlink", () => {
  const hyperlinkExternal = mount(Hyperlink, {
    props: {
      to: "https://molgenis.org",
      type: "external",
    },
  });

  test("External links should have specific rels for security/privacy", async () => {
    expect(hyperlinkExternal.html()).toContain(
      'rel="external noopener noreferrer"'
    );
  });

  test("External links should have icon to indicate it is external", async () => {
    expect(hyperlinkExternal.findComponent(BaseIcon).props()["name"]).toBe(
      "ExternalLink"
    );
  });
});
