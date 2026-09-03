import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import OntologyRow from "../../../../app/components/display/OntologyRow.vue";

async function mountRow(props: Record<string, unknown>) {
  const wrapper = mount(OntologyRow, { props: { name: "Term", ...props } });
  await flushPromises();
  return wrapper;
}

function markersPresent(wrapper: Awaited<ReturnType<typeof mountRow>>) {
  return {
    caret: wrapper.find('[data-marker="caret"]').exists(),
    bullet: wrapper.find('[data-marker="bullet"]').exists(),
    connector: wrapper.find('[data-marker="connector"]').exists(),
  };
}

function gutter(wrapper: Awaited<ReturnType<typeof mountRow>>) {
  return wrapper.find("span.relative.w-5.h-5.shrink-0");
}

describe("display/OntologyRow.vue marker", () => {
  it("shows the caret and no other marker when the row has children", async () => {
    const wrapper = await mountRow({
      hasChildren: true,
      marker: "connector",
    });
    expect(markersPresent(wrapper)).toEqual({
      caret: true,
      bullet: false,
      connector: false,
    });
  });

  it("shows a bullet, rendered as a plain element and no other marker, for a flat-list row", async () => {
    const wrapper = await mountRow({ marker: "bullet" });
    expect(markersPresent(wrapper)).toEqual({
      caret: false,
      bullet: true,
      connector: false,
    });
    expect(wrapper.find('[data-marker="bullet"]').element.tagName).toBe("SPAN");
  });

  it("shows the elbow connector and no other marker for a non-root tree leaf", async () => {
    const wrapper = await mountRow({ marker: "connector" });
    expect(markersPresent(wrapper)).toEqual({
      caret: false,
      bullet: false,
      connector: true,
    });
    // The asset's dashed arm sits 10px below its own box centre; the extra
    // 10px in the y offset lands the arm, not the box, on the gutter centre.
    expect(wrapper.find('[data-marker="connector"]').classes()).toContain(
      "-translate-y-[calc(50%+10px)]"
    );
  });

  it("renders no gutter at all for the standalone flush value, text sits flush left", async () => {
    const wrapper = await mountRow({ marker: "flush" });
    expect(markersPresent(wrapper)).toEqual({
      caret: false,
      bullet: false,
      connector: false,
    });
    expect(gutter(wrapper).exists()).toBe(false);
  });

  it("renders an empty but reserved gutter for a tree root leaf", async () => {
    const wrapper = await mountRow({ marker: "blank" });
    expect(markersPresent(wrapper)).toEqual({
      caret: false,
      bullet: false,
      connector: false,
    });
    expect(gutter(wrapper).exists()).toBe(true);
  });

  it("keeps a blank row's text starting at the same x as a caret row's", async () => {
    const blank = await mountRow({ marker: "blank" });
    const caret = await mountRow({ hasChildren: true });

    // Same fixed-width gutter class list in both, and in both it is the
    // name span's immediate previous sibling: text starts at the same x
    // by construction, not by a pixel measurement jsdom cannot make.
    expect(gutter(blank).classes()).toEqual(gutter(caret).classes());
    expect(
      blank.find("span.relative.w-5.h-5.shrink-0 + span.flex").exists()
    ).toBe(true);
    expect(
      caret.find("span.relative.w-5.h-5.shrink-0 + span.flex").exists()
    ).toBe(true);
  });
});

describe("display/OntologyRow.vue gutter contributes nothing to row height", () => {
  // Pins the fix for a bug seen twice: a marker's own intrinsic size (a
  // 24x24 caret button, a 22px-tall connector icon) fed back into the row's
  // height because it sat in normal flow. The gutter is fixed-size and every
  // marker is positioned, not flowed, so the row's height can only ever
  // come from the text line box.
  it.each([
    ["caret", { hasChildren: true }],
    ["bullet", { marker: "bullet" as const }],
    ["connector", { marker: "connector" as const }],
  ])(
    "keeps the fixed-size gutter box for the %s marker",
    async (markerName, props) => {
      const wrapper = await mountRow(props);
      const gutter = wrapper.find("span.relative");
      expect(gutter.classes()).toEqual(
        expect.arrayContaining(["relative", "w-5", "h-5", "shrink-0"])
      );
      expect(wrapper.find(`[data-marker="${markerName}"]`).classes()).toContain(
        "absolute"
      );
    }
  );
});
