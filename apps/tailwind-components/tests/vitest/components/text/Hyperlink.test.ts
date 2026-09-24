import { mountSuspended } from "@nuxt/test-utils/runtime";
import { expect, test } from "vitest";
import Hyperlink from "../../../../app/components/text/Hyperlink.vue";
import BaseIcon from "../../../../app/components/BaseIcon.vue";

test("External links should have specific rels for security/privacy", async () => {
  const hyperlinkExternal = await mountSuspended(Hyperlink, {
    props: {
      to: "https://molgenis.org",
      type: "external",
    },
  });
  expect(hyperlinkExternal.html()).toContain(
    'rel="external noopener noreferrer"'
  );
});

test("External links should have icon to indicate it is external", async () => {
  const hyperlinkExternal = await mountSuspended(Hyperlink, {
    props: {
      to: "https://molgenis.org",
      type: "external",
    },
  });
  expect(hyperlinkExternal.findComponent(BaseIcon).props()["name"]).toBe(
    "ExternalLink"
  );
});
