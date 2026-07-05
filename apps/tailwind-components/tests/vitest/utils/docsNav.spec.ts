import { describe, it, expect } from "vitest";
import { readdirSync, statSync } from "fs";
import { join, relative } from "path";
import { buildDocsTree } from "../../../app/utils/docsNav";
import type {
  LegendSection,
  LegendHeading,
} from "../../../../metadata-utils/src/types";

const PAGES_DIR = join(__dirname, "../../../app/pages");

function collectStoryPaths(dir: string, base: string): string[] {
  const results: string[] = [];
  for (const entry of readdirSync(dir)) {
    const fullPath = join(dir, entry);
    const stat = statSync(fullPath);
    if (stat.isDirectory()) {
      results.push(...collectStoryPaths(fullPath, base));
    } else if (entry.endsWith(".story.vue")) {
      const rel = relative(base, fullPath).replace(/\\/g, "/");
      results.push(`../pages/${rel}`);
    }
  }
  return results;
}

function collectAllLeafIds(sections: LegendSection[]): string[] {
  return sections.flatMap((section) =>
    section.headers.flatMap((header) =>
      header.children ? header.children.map((child) => child.id) : [header.id]
    )
  );
}

function findSection(
  sections: LegendSection[],
  label: string
): LegendSection | undefined {
  return sections.find((section) => section.label === label);
}

function findGroup(
  sections: LegendSection[],
  groupLabel: string
): LegendHeading | undefined {
  const components = findSection(sections, "Components");
  return components?.headers.find((h) => h.label === groupLabel);
}

function groupChildIds(
  sections: LegendSection[],
  groupLabel: string
): string[] {
  return findGroup(sections, groupLabel)?.children?.map((c) => c.id) ?? [];
}

function foundationHeaderIds(sections: LegendSection[]): string[] {
  return findSection(sections, "Foundations")?.headers.map((h) => h.id) ?? [];
}

function examplesHeaderIds(sections: LegendSection[]): string[] {
  return (
    findSection(sections, "Examples & prototypes")?.headers.map((h) => h.id) ??
    []
  );
}

function resolveIsActive(item: {
  isActive: boolean | { value: boolean };
}): boolean {
  return typeof item.isActive === "boolean"
    ? item.isActive
    : item.isActive.value;
}

const allRealPaths = collectStoryPaths(PAGES_DIR, PAGES_DIR);

describe("buildDocsTree top-level structure", () => {
  it("covers all 75 real story files (exhaustive fixture check)", () => {
    expect(allRealPaths).toHaveLength(75);
  });

  it("returns exactly 3 top-level sections in order: Foundations, Components, Examples & prototypes", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(sections).toHaveLength(3);
    expect(sections[0]!.label).toBe("Foundations");
    expect(sections[1]!.label).toBe("Components");
    expect(sections[2]!.label).toBe("Examples & prototypes");
  });

  it("Get started is NOT present", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(findSection(sections, "Get started")).toBeUndefined();
  });

  it("section ids are correct", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(sections[0]!.id).toBe("/section/foundations");
    expect(sections[1]!.id).toBe("/section/components");
    expect(sections[2]!.id).toBe("/section/examples-prototypes");
  });

  it("all sections have type SECTION", () => {
    const sections = buildDocsTree(allRealPaths, "");
    for (const section of sections) {
      expect(section.type).toBe("SECTION");
    }
  });

  it("all group headings in Components have type HEADING", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const components = findSection(sections, "Components")!;
    for (const header of components.headers) {
      expect(header.type).toBe("HEADING");
    }
  });

  it("all leaf headings (component children) have type HEADING", () => {
    const sections = buildDocsTree(allRealPaths, "");
    for (const section of sections) {
      for (const header of section.headers) {
        if (header.children) {
          for (const child of header.children) {
            expect(child.type).toBe("HEADING");
          }
        }
      }
    }
  });
});

describe("buildDocsTree Components section groups", () => {
  it("Components has exactly 11 group headers in config order", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const groups = findSection(sections, "Components")!.headers;
    expect(groups).toHaveLength(11);
    expect(groups[0]!.label).toBe("Actions");
    expect(groups[1]!.label).toBe("Inputs");
    expect(groups[2]!.label).toBe("Filter");
    expect(groups[3]!.label).toBe("Feedback");
    expect(groups[4]!.label).toBe("Overlays");
    expect(groups[5]!.label).toBe("Navigation");
    expect(groups[6]!.label).toBe("Display");
    expect(groups[7]!.label).toBe("Containers");
    expect(groups[8]!.label).toBe("Page layouts");
    expect(groups[9]!.label).toBe("Visualisation");
    expect(groups[10]!.label).toBe("EMX2");
  });

  it("group headers have overview route ids", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const components = findSection(sections, "Components")!;
    const actionsGroup = components.headers.find((h) => h.label === "Actions");
    expect(actionsGroup?.id).toBe("/section/actions");
    const containersGroup = components.headers.find(
      (h) => h.label === "Containers"
    );
    expect(containersGroup?.id).toBe("/section/containers");
    const pageLayoutsGroup = components.headers.find(
      (h) => h.label === "Page layouts"
    );
    expect(pageLayoutsGroup?.id).toBe("/section/page-layouts");
  });

  it("group headers all have children arrays", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const groups = findSection(sections, "Components")!.headers;
    for (const group of groups) {
      expect(Array.isArray(group.children)).toBe(true);
      expect(group.children!.length).toBeGreaterThan(0);
    }
  });

  it("Foundations headers have no children (2-level, curated order)", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const foundations = findSection(sections, "Foundations")!;
    for (const header of foundations.headers) {
      expect(header.children).toBeUndefined();
    }
    const labels = foundations.headers.map((h) => h.label);
    expect(labels[0]).toBe("Colors");
    expect(labels[1]).toBe("Typography");
    expect(labels[2]).toBe("Spacing");
    expect(labels[3]).toBe("Radius & elevation");
    expect(labels[4]).toBe("Icons");
    expect(labels[5]).toBe("Theme styles overview");
    expect(labels[6]).toBe("Theme switch");
  });

  it("Examples & prototypes headers have no children and are in curated order", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const examples = findSection(sections, "Examples & prototypes")!;
    for (const header of examples.headers) {
      expect(header.children).toBeUndefined();
    }
    const labels = examples.headers.map((h) => h.label);
    expect(labels[0]).toBe("Row edit");
    expect(labels[1]).toBe("Edit modal");
    expect(examplesHeaderIds(sections)).toContain("/samples/rowEdit");
    expect(examplesHeaderIds(sections)).toContain("/samples/formModal");
  });
});

describe("buildDocsTree group memberships", () => {
  it("Actions contains Button, ButtonBar, ButtonDropdown, Switch", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Actions");
    expect(ids).toContain("/Button.story");
    expect(ids).toContain("/ButtonBar.story");
    expect(ids).toContain("/ButtonDropdown.story");
    expect(ids).toContain("/input/Switch.story");
  });

  it("Switch is in Actions not Inputs (prior decision, red-green guard)", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(groupChildIds(sections, "Actions")).toContain("/input/Switch.story");
    expect(groupChildIds(sections, "Inputs")).not.toContain(
      "/input/Switch.story"
    );
  });

  it("Inputs contains dumb inputs (String, Boolean, Array) but not Ref/RefBack/RefSelect/Ontology/Switch/FilterSearch", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Inputs");
    expect(ids).toContain("/input/String.story");
    expect(ids).toContain("/input/Boolean.story");
    expect(ids).toContain("/input/Array.story");
    expect(ids).not.toContain("/input/Ref.story");
    expect(ids).not.toContain("/input/RefBack.story");
    expect(ids).not.toContain("/input/RefSelect.story");
    expect(ids).not.toContain("/input/Ontology.story");
    expect(ids).not.toContain("/input/Switch.story");
    expect(ids).not.toContain("/FilterSearch.story");
  });

  it("Filter group contains FilterSearch and FilterSystem", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Filter");
    expect(ids).toContain("/FilterSearch.story");
    expect(ids).toContain("/filter/FilterSystem.story");
  });

  it("FilterSearch is in Filter NOT Inputs; FilterSystem is in Filter NOT EMX2", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(groupChildIds(sections, "Filter")).toContain("/FilterSearch.story");
    expect(groupChildIds(sections, "Inputs")).not.toContain(
      "/FilterSearch.story"
    );
    expect(groupChildIds(sections, "Filter")).toContain(
      "/filter/FilterSystem.story"
    );
    expect(groupChildIds(sections, "EMX2")).not.toContain(
      "/filter/FilterSystem.story"
    );
  });

  it("Feedback contains Banner (root), Message, Error, RequiredInfoSection, Draft — NOT Skeleton", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Feedback");
    expect(ids).toContain("/Banner.story");
    expect(ids).toContain("/Message.story");
    expect(ids).toContain("/form/Error.story");
    expect(ids).toContain("/form/RequiredInfoSection.story");
    expect(ids).toContain("/label/Draft.story");
    expect(ids).not.toContain("/Skeleton.story");
  });

  it("Overlays contains Modal, SideModal, CustomTooltip", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Overlays");
    expect(ids).toContain("/Modal.story");
    expect(ids).toContain("/SideModal.story");
    expect(ids).toContain("/CustomTooltip.story");
  });

  it("Navigation contains Navigation, NavigationNested, BreadCrumbs, Pagination, NavigationGroups, Legend — NOT Header", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Navigation");
    expect(ids).toContain("/Navigation.story");
    expect(ids).toContain("/NavigationNested.story");
    expect(ids).toContain("/BreadCrumbs.story");
    expect(ids).toContain("/Pagination.story");
    expect(ids).toContain("/pages/NavigationGroups.story");
    expect(ids).toContain("/Legend.story");
    expect(ids).not.toContain("/Header.story");
  });

  it("Display contains DisplayList, Image, Logo, LogoMobile, Icons, Heading, Paragraph, text/Hyperlink — NOT pages/Banner or Header", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Display");
    expect(ids).toContain("/DisplayList.story");
    expect(ids).toContain("/Image.story");
    expect(ids).toContain("/Logo.story");
    expect(ids).toContain("/LogoMobile.story");
    expect(ids).toContain("/Icons.story");
    expect(ids).toContain("/pages/Heading.story");
    expect(ids).toContain("/pages/Paragraph.story");
    expect(ids).toContain("/text/Hyperlink.story");
    expect(ids).not.toContain("/pages/Banner.story");
    expect(ids).not.toContain("/Header.story");
  });

  it("Containers contains Accordion, ShowMore, Skeleton, SlideUp — NOT PageHeader, FooterComponent, or (old) Feedback Skeleton", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Containers");
    expect(ids).toContain("/Accordion.story");
    expect(ids).toContain("/ShowMore.story");
    expect(ids).toContain("/Skeleton.story");
    expect(ids).toContain("/transition/SlideUp.story");
    expect(ids).not.toContain("/PageHeader.story");
    expect(ids).not.toContain("/FooterComponent.story");
  });

  it("Skeleton is in Containers, NOT Feedback (moved from Feedback → Containers)", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(groupChildIds(sections, "Containers")).toContain("/Skeleton.story");
    expect(groupChildIds(sections, "Feedback")).not.toContain(
      "/Skeleton.story"
    );
  });

  it("Page layouts (new) contains Header, PageHeader, FooterComponent, pages/Banner (labeled 'Page banner')", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Page layouts");
    expect(ids).toContain("/Header.story");
    expect(ids).toContain("/PageHeader.story");
    expect(ids).toContain("/FooterComponent.story");
    expect(ids).toContain("/pages/Banner.story");
    const group = findGroup(sections, "Page layouts")!;
    const pageBanner = group.children!.find(
      (c) => c.id === "/pages/Banner.story"
    );
    expect(pageBanner?.label).toBe("Page banner");
  });

  it("Header is in Page layouts, NOT Navigation (moved from Navigation → Page layouts)", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(groupChildIds(sections, "Page layouts")).toContain("/Header.story");
    expect(groupChildIds(sections, "Navigation")).not.toContain(
      "/Header.story"
    );
  });

  it("pages/Banner is in Page layouts, NOT Display (moved from Display → Page layouts, red-green guard)", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(groupChildIds(sections, "Page layouts")).toContain(
      "/pages/Banner.story"
    );
    expect(groupChildIds(sections, "Display")).not.toContain(
      "/pages/Banner.story"
    );
  });

  it("pages/Banner/Display guard with minimal paths", () => {
    const testPaths = [
      "../pages/Banner.story.vue",
      "../pages/pages/Banner.story.vue",
    ];
    const sections = buildDocsTree(testPaths, "");
    expect(groupChildIds(sections, "Page layouts")).toContain(
      "/pages/Banner.story"
    );
    expect(groupChildIds(sections, "Feedback")).toContain("/Banner.story");
    expect(groupChildIds(sections, "Feedback")).not.toContain(
      "/pages/Banner.story"
    );
  });

  it("Visualisation contains viz stories", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "Visualisation");
    expect(ids).toContain("/viz/ColumnChart.story");
    expect(ids).toContain("/viz/PieChart.story");
  });

  it("EMX2 contains Form, Field, AddModal, EditModal, Ref, RefBack, RefSelect, Ontology, table stories, Session — NOT FilterSystem", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const ids = groupChildIds(sections, "EMX2");
    expect(ids).toContain("/Form.story");
    expect(ids).toContain("/Field.story");
    expect(ids).toContain("/form/AddModal.story");
    expect(ids).toContain("/form/EditModal.story");
    expect(ids).toContain("/input/Ref.story");
    expect(ids).toContain("/input/RefBack.story");
    expect(ids).toContain("/input/RefSelect.story");
    expect(ids).toContain("/input/Ontology.story");
    expect(ids).toContain("/table/EMX2.story");
    expect(ids).toContain("/table/modal/Ref.story");
    expect(ids).toContain("/Session.story");
    expect(ids).not.toContain("/filter/FilterSystem.story");
  });

  it("two-pass: EMX2 claims Ref/RefBack/RefSelect/Ontology before Inputs dir sweep", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const emx2Ids = groupChildIds(sections, "EMX2");
    const inputsIds = groupChildIds(sections, "Inputs");
    expect(emx2Ids).toContain("/input/Ref.story");
    expect(emx2Ids).toContain("/input/RefBack.story");
    expect(emx2Ids).toContain("/input/RefSelect.story");
    expect(emx2Ids).toContain("/input/Ontology.story");
    expect(inputsIds).not.toContain("/input/Ref.story");
    expect(inputsIds).not.toContain("/input/RefBack.story");
    expect(inputsIds).not.toContain("/input/RefSelect.story");
    expect(inputsIds).not.toContain("/input/Ontology.story");
  });

  it("EMX2 table stories are labeled 'Table' and 'Table ref'", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const group = findGroup(sections, "EMX2")!;
    const tableEntry = group.children!.find(
      (c) => c.id === "/table/EMX2.story"
    );
    expect(tableEntry?.label).toBe("Table");
    const tableRefEntry = group.children!.find(
      (c) => c.id === "/table/modal/Ref.story"
    );
    expect(tableRefEntry?.label).toBe("Table ref");
  });

  it("Icons in Display is labeled 'Icon component'", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const group = findGroup(sections, "Display")!;
    const iconsEntry = group.children!.find((c) => c.id === "/Icons.story");
    expect(iconsEntry?.label).toBe("Icon component");
  });

  it("ThemeSwitch is in Foundations headers, not in any Components group", () => {
    const sections = buildDocsTree(allRealPaths, "");
    expect(foundationHeaderIds(sections)).toContain("/ThemeSwitch.story");
    const components = findSection(sections, "Components")!;
    for (const group of components.headers) {
      expect(group.children?.map((c) => c.id) ?? []).not.toContain(
        "/ThemeSwitch.story"
      );
    }
  });
});

describe("buildDocsTree conservation", () => {
  it("every real story appears exactly once as a leaf", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const leafIds = collectAllLeafIds(sections);
    const storyRoutes = allRealPaths.map((p) =>
      p.replace("../pages/", "/").replace(".vue", "")
    );
    for (const route of storyRoutes) {
      const count = leafIds.filter((id) => id === route).length;
      expect(count, `route ${route} should appear exactly once`).toBe(1);
    }
  });

  it("no Ungrouped group when all 75 real stories match a group", () => {
    const sections = buildDocsTree(allRealPaths, "");
    const components = findSection(sections, "Components")!;
    const ungrouped = components.headers.find((h) => h.label === "Ungrouped");
    expect(ungrouped).toBeUndefined();
  });

  it("Ungrouped appears under Components when a story has no group match", () => {
    const testPaths = [...allRealPaths, "../pages/unknowndir/Widget.story.vue"];
    const sections = buildDocsTree(testPaths, "");
    const components = findSection(sections, "Components")!;
    const ungrouped = components.headers.find((h) => h.label === "Ungrouped");
    expect(ungrouped).toBeDefined();
    expect(ungrouped?.children?.map((c) => c.id)).toContain(
      "/unknowndir/Widget.story"
    );
  });

  it("Ungrouped group has empty id", () => {
    const testPaths = [...allRealPaths, "../pages/unknowndir/Orphan.story.vue"];
    const sections = buildDocsTree(testPaths, "");
    const components = findSection(sections, "Components")!;
    const ungrouped = components.headers.find((h) => h.label === "Ungrouped");
    expect(ungrouped?.id).toBe("");
  });

  it("root-level story with no match falls into Ungrouped", () => {
    const testPaths = [...allRealPaths, "../pages/UnknownWidget.story.vue"];
    const sections = buildDocsTree(testPaths, "");
    const components = findSection(sections, "Components")!;
    const ungrouped = components.headers.find((h) => h.label === "Ungrouped");
    expect(ungrouped?.children?.map((c) => c.id)).toContain(
      "/UnknownWidget.story"
    );
  });

  it("section order is deterministic regardless of input path order", () => {
    const shuffled = [...allRealPaths].reverse();
    const sectionsOriginal = buildDocsTree(allRealPaths, "");
    const sectionsShuffled = buildDocsTree(shuffled, "");
    const labelsOriginal = sectionsOriginal.map((s) => s.label);
    const labelsShuffled = sectionsShuffled.map((s) => s.label);
    expect(labelsOriginal).toEqual(labelsShuffled);
    const groupsOriginal = findSection(
      sectionsOriginal,
      "Components"
    )!.headers.map((h) => h.label);
    const groupsShuffled = findSection(
      sectionsShuffled,
      "Components"
    )!.headers.map((h) => h.label);
    expect(groupsOriginal).toEqual(groupsShuffled);
  });

  it("dir-matched children sorted alphabetically regardless of input order", () => {
    const unordered = [
      "../pages/viz/PieChart.story.vue",
      "../pages/viz/ColumnChart.story.vue",
      "../pages/viz/ProgressMeter.story.vue",
      "../pages/viz/ChartLegend.story.vue",
    ];
    const reversed = [...unordered].reverse();
    const labelsOriginal =
      findGroup(buildDocsTree(unordered, ""), "Visualisation")?.children?.map(
        (c) => c.label
      ) ?? [];
    const labelsReversed =
      findGroup(buildDocsTree(reversed, ""), "Visualisation")?.children?.map(
        (c) => c.label
      ) ?? [];
    expect(labelsOriginal).toEqual(labelsReversed);
    const sorted = [...labelsOriginal].sort((a, b) => a.localeCompare(b));
    expect(labelsOriginal).toEqual(sorted);
  });
});

describe("buildDocsTree isActive", () => {
  it("isActive is a plain boolean on all items when path is empty string", () => {
    const sections = buildDocsTree(allRealPaths, "");
    for (const section of sections) {
      expect(typeof section.isActive).toBe("boolean");
      for (const header of section.headers) {
        expect(typeof header.isActive).toBe("boolean");
        for (const child of header.children ?? []) {
          expect(typeof child.isActive).toBe("boolean");
        }
      }
    }
  });

  it("all isActive values are false when currentPath is empty", () => {
    const sections = buildDocsTree(allRealPaths, "");
    for (const section of sections) {
      expect(resolveIsActive(section)).toBe(false);
      for (const header of section.headers) {
        expect(resolveIsActive(header)).toBe(false);
        for (const child of header.children ?? []) {
          expect(resolveIsActive(child)).toBe(false);
        }
      }
    }
  });

  it("isActive true on a leaf, its group, and Components section when on a component route", () => {
    const sections = buildDocsTree(allRealPaths, "/viz/ColumnChart.story");
    const componentsSection = findSection(sections, "Components")!;
    expect(resolveIsActive(componentsSection)).toBe(true);
    const vizGroup = findGroup(sections, "Visualisation")!;
    expect(resolveIsActive(vizGroup)).toBe(true);
    const leaf = vizGroup.children!.find(
      (c) => c.id === "/viz/ColumnChart.story"
    )!;
    expect(resolveIsActive(leaf)).toBe(true);
    for (const otherChild of vizGroup.children!.filter(
      (c) => c.id !== "/viz/ColumnChart.story"
    )) {
      expect(resolveIsActive(otherChild)).toBe(false);
    }
    expect(resolveIsActive(findSection(sections, "Foundations")!)).toBe(false);
    expect(
      resolveIsActive(findSection(sections, "Examples & prototypes")!)
    ).toBe(false);
  });

  it("isActive true on Foundations section and matching header when on a foundation route", () => {
    const sections = buildDocsTree(allRealPaths, "/foundations/colors");
    const foundations = findSection(sections, "Foundations")!;
    expect(resolveIsActive(foundations)).toBe(true);
    const colorsHeader = foundations.headers.find(
      (h) => h.id === "/foundations/colors"
    )!;
    expect(resolveIsActive(colorsHeader)).toBe(true);
    expect(resolveIsActive(findSection(sections, "Components")!)).toBe(false);
  });

  it("isActive true on Components and matching group when on a group overview route", () => {
    const sections = buildDocsTree(allRealPaths, "/section/actions");
    const components = findSection(sections, "Components")!;
    expect(resolveIsActive(components)).toBe(true);
    const actionsGroup = findGroup(sections, "Actions")!;
    expect(resolveIsActive(actionsGroup)).toBe(true);
    for (const child of actionsGroup.children ?? []) {
      expect(resolveIsActive(child)).toBe(false);
    }
    expect(resolveIsActive(findSection(sections, "Foundations")!)).toBe(false);
  });

  it("isActive true on Foundations when on /section/foundations", () => {
    const sections = buildDocsTree(allRealPaths, "/section/foundations");
    expect(resolveIsActive(findSection(sections, "Foundations")!)).toBe(true);
    expect(resolveIsActive(findSection(sections, "Components")!)).toBe(false);
  });

  it("isActive true on Components when on /section/components", () => {
    const sections = buildDocsTree(allRealPaths, "/section/components");
    expect(resolveIsActive(findSection(sections, "Components")!)).toBe(true);
    expect(resolveIsActive(findSection(sections, "Foundations")!)).toBe(false);
  });

  it("isActive true on Examples & prototypes when on /section/examples-prototypes", () => {
    const sections = buildDocsTree(
      allRealPaths,
      "/section/examples-prototypes"
    );
    expect(
      resolveIsActive(findSection(sections, "Examples & prototypes")!)
    ).toBe(true);
    expect(resolveIsActive(findSection(sections, "Foundations")!)).toBe(false);
  });

  it("isActive true on EMX2 group when on a smart-input route (Ref)", () => {
    const sections = buildDocsTree(allRealPaths, "/input/Ref.story");
    expect(resolveIsActive(findGroup(sections, "EMX2")!)).toBe(true);
    expect(resolveIsActive(findGroup(sections, "Inputs")!)).toBe(false);
    expect(resolveIsActive(findSection(sections, "Components")!)).toBe(true);
  });

  it("isActive true on Examples when on a sample route", () => {
    const sections = buildDocsTree(allRealPaths, "/samples/rowEdit");
    const examples = findSection(sections, "Examples & prototypes")!;
    expect(resolveIsActive(examples)).toBe(true);
    const rowEditHeader = examples.headers.find(
      (h) => h.id === "/samples/rowEdit"
    )!;
    expect(resolveIsActive(rowEditHeader)).toBe(true);
  });

  it("errorCount is always 0 and isVisible always true", () => {
    const sections = buildDocsTree(allRealPaths, "");
    for (const section of sections) {
      expect(section.errorCount.value).toBe(0);
      expect(section.isVisible.value).toBe(true);
      for (const header of section.headers) {
        expect(header.errorCount.value).toBe(0);
        expect(header.isVisible.value).toBe(true);
        for (const child of header.children ?? []) {
          expect(child.errorCount.value).toBe(0);
          expect(child.isVisible.value).toBe(true);
        }
      }
    }
  });
});

describe("buildDocsTree search filter", () => {
  it("empty query returns all 3 sections with all groups and children", () => {
    const sections = buildDocsTree(allRealPaths, "", "");
    expect(sections).toHaveLength(3);
    const groups = findSection(sections, "Components")!.headers;
    expect(groups).toHaveLength(11);
  });

  it("query 'ref' returns EMX2 with Ref/RefBack/RefSelect; Actions section absent", () => {
    const sections = buildDocsTree(allRealPaths, "", "ref");
    const components = findSection(sections, "Components");
    expect(components).toBeDefined();
    const emx2Group = components?.headers.find((h) => h.label === "EMX2");
    expect(emx2Group).toBeDefined();
    const childLabels = emx2Group?.children?.map((c) => c.label) ?? [];
    expect(childLabels).toContain("Ref");
    expect(childLabels).toContain("RefBack");
    expect(childLabels).toContain("RefSelect");
    const actionsGroup = components?.headers.find((h) => h.label === "Actions");
    expect(actionsGroup).toBeUndefined();
  });

  it("all matching children satisfy the query regexp", () => {
    const sections = buildDocsTree(allRealPaths, "", "ref");
    for (const section of sections) {
      for (const header of section.headers) {
        if (header.children) {
          for (const child of header.children) {
            expect(child.label).toMatch(/ref/i);
          }
        }
      }
    }
  });

  it("multi-word AND query narrows to specific children", () => {
    const sections = buildDocsTree(allRealPaths, "", "column chart");
    const allChildren = sections.flatMap((s) =>
      s.headers.flatMap((h) => h.children ?? [])
    );
    expect(allChildren.some((c) => c.label === "ColumnChart")).toBe(true);
    expect(
      allChildren.every(
        (c) => /column/i.test(c.label) && /chart/i.test(c.label)
      )
    ).toBe(true);
  });

  it("query matching a GROUP name returns that group with all its children", () => {
    const sections = buildDocsTree(allRealPaths, "", "Visualisation");
    const components = findSection(sections, "Components")!;
    const vizGroup = components.headers.find(
      (h) => h.label === "Visualisation"
    );
    expect(vizGroup).toBeDefined();
    expect(vizGroup!.children!.length).toBeGreaterThan(0);
  });

  it("query matching a SECTION name returns that section with all groups", () => {
    const sections = buildDocsTree(allRealPaths, "", "Foundations");
    const foundations = findSection(sections, "Foundations");
    expect(foundations).toBeDefined();
    expect(foundations!.headers.length).toBeGreaterThan(0);
    expect(findSection(sections, "Components")).toBeUndefined();
  });

  it("invalid-regex word does not throw and falls back to literal match", () => {
    expect(() => buildDocsTree(allRealPaths, "", "(")).not.toThrow();
    const result = buildDocsTree(allRealPaths, "", "(");
    expect(Array.isArray(result)).toBe(true);
  });
});

