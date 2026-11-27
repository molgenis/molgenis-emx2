import { mountSuspended } from "@nuxt/test-utils/runtime";
import { describe, expect, it } from "vitest";
import { h } from "vue";
import Header from "../../../app/components/Header.vue";

describe("Header", () => {
  it("renders a row for small, medium and large screens", async () => {
    const wrapper = await mountSuspended(Header);

    expect(wrapper.find('[data-testid="header-large"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="header-medium"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="header-small"]').exists()).toBe(true);
  });

  it("asks for an icon-only account button only on small screens", async () => {
    const wrapper = await mountSuspended(Header, {
      slots: {
        account: ({ iconOnly }: { iconOnly: boolean }) =>
          h("span", { class: "account" }, iconOnly ? "icon" : "label"),
      },
    });

    const accountIn = (row: string) =>
      wrapper.find(`[data-testid="header-${row}"] .account`).text();
    expect(accountIn("large")).toBe("label");
    expect(accountIn("medium")).toBe("label");
    expect(accountIn("small")).toBe("icon");
  });

  it("only renders the small screen menu row when it is given", async () => {
    const withoutRow = await mountSuspended(Header);
    expect(
      withoutRow.find('[data-testid="header-small"]').text()
    ).not.toContain("Shortcuts");

    const withRow = await mountSuspended(Header, {
      slots: { "nav-mobile-row": () => h("nav", "Shortcuts") },
    });
    expect(withRow.find('[data-testid="header-small"]').text()).toContain(
      "Shortcuts"
    );
  });
});
