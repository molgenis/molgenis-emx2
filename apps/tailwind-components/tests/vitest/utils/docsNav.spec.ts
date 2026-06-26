import { describe, it, expect } from "vitest";
import { readdirSync, statSync } from "fs";
import { join, relative } from "path";
import {
  buildDocsLegend,
  buildDocsSidebar,
  getSectionNavForRoute,
  getSectionOverview,
  getSectionTitleBySlug,
} from "../../../app/utils/docsNav";
import type { LegendSection } from "../../../../metadata-utils/src/types";

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

function collectAllStoryRoutes(sections: LegendSection[]): string[] {
  return sections.flatMap((section) =>
    section.headers.map((header) => header.id)
  );
}

function findSection(
  sections: LegendSection[],
  label: string
): LegendSection | undefined {
  return sections.find((section) => section.label === label);
}

function sectionHeaderIds(sections: LegendSection[], label: string): string[] {
  return (findSection(sections, label)?.headers ?? []).map(
    (header) => header.id
  );
}

const allRealPaths = collectStoryPaths(PAGES_DIR, PAGES_DIR);

describe("buildDocsLegend", () => {
  it("top-level sections are present in the correct order", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const labels = sections.map((section) => section.label);
    expect(labels[0]).toBe("Get started");
    expect(labels[1]).toBe("Foundations");
    expect(labels[2]).toBe("Actions");
    expect(labels[3]).toBe("Inputs");
    expect(labels[4]).toBe("Navigation");
    expect(labels[5]).toBe("Feedback");
    expect(labels[6]).toBe("Overlays");
    expect(labels[7]).toBe("Display");
    expect(labels[8]).toBe("Layout");
    expect(labels[9]).toBe("Visualisation");
    expect(labels[10]).toBe("EMX2");
    expect(labels[11]).toBe("Page templates");
    expect(labels[12]).toBe("Prototypes");
    expect(labels[13]).toBe("Data fetching");
  });

  it("Forms group is gone", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    expect(findSection(sections, "Forms")).toBeUndefined();
  });

  it("all sections have type SECTION", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      expect(section.type).toBe("SECTION");
    }
  });

  it("all headings have type HEADING", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      for (const header of section.headers) {
        expect(header.type).toBe("HEADING");
      }
    }
  });

  it("covers all 72 real story files (exhaustive fixture check)", () => {
    expect(allRealPaths).toHaveLength(72);
  });

  it("every real story path appears exactly once across all section headers (conservation)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const allRoutes = collectAllStoryRoutes(sections);
    const storyRoutes = allRealPaths.map((path) =>
      path.replace("../pages/", "/").replace(".vue", "")
    );
    for (const route of storyRoutes) {
      const count = allRoutes.filter((id) => id === route).length;
      expect(count, `route ${route} should appear exactly once`).toBe(1);
    }
  });

  it("every input story id appears exactly once (conservation, 72 count)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const allRoutes = collectAllStoryRoutes(sections);
    const storyRoutes = allRealPaths.map((path) =>
      path.replace("../pages/", "/").replace(".vue", "")
    );
    expect(storyRoutes).toHaveLength(72);
    for (const storyRoute of storyRoutes) {
      const count = allRoutes.filter((id) => id === storyRoute).length;
      expect(count, `route ${storyRoute} must appear exactly once`).toBe(1);
    }
  });

  it("Ungrouped is absent when every story matches a group", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const ungrouped = findSection(sections, "Ungrouped");
    expect(ungrouped).toBeUndefined();
  });

  it("ThemeSwitch is in Foundations headers and NOT in any group or Ungrouped", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const foundationsIds = sectionHeaderIds(sections, "Foundations");
    expect(foundationsIds).toContain("/ThemeSwitch.story");
    const groupLabels = [
      "Actions",
      "Inputs",
      "Navigation",
      "Feedback",
      "Overlays",
      "Display",
      "Layout",
      "Visualisation",
      "EMX2",
      "Ungrouped",
    ];
    for (const label of groupLabels) {
      const ids = sectionHeaderIds(sections, label);
      expect(ids, `ThemeSwitch must not appear in ${label}`).not.toContain(
        "/ThemeSwitch.story"
      );
    }
  });

  it("Switch lands in Actions (not Inputs) because Actions is processed first", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const actionsIds = sectionHeaderIds(sections, "Actions");
    const inputsIds = sectionHeaderIds(sections, "Inputs");
    expect(actionsIds).toContain("/input/Switch.story");
    expect(inputsIds).not.toContain("/input/Switch.story");
  });

  it("Accordion lands in Layout headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const ids = sectionHeaderIds(sections, "Layout");
    expect(ids).toContain("/Accordion.story");
  });

  it("PageHeader and FooterComponent land in Layout, not Navigation", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const layoutIds = sectionHeaderIds(sections, "Layout");
    const navIds = sectionHeaderIds(sections, "Navigation");
    expect(layoutIds).toContain("/PageHeader.story");
    expect(layoutIds).toContain("/FooterComponent.story");
    expect(navIds).not.toContain("/PageHeader.story");
    expect(navIds).not.toContain("/FooterComponent.story");
  });

  it("Legend lands in Navigation headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const ids = sectionHeaderIds(sections, "Navigation");
    expect(ids).toContain("/Legend.story");
  });

  it("Session is in EMX2, not Navigation", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    const navIds = sectionHeaderIds(sections, "Navigation");
    expect(emx2Ids).toContain("/Session.story");
    expect(navIds).not.toContain("/Session.story");
  });

  it("EMX2 group contains Form, Field, AddModal, EditModal, Session", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    expect(emx2Ids).toContain("/Form.story");
    expect(emx2Ids).toContain("/Field.story");
    expect(emx2Ids).toContain("/form/AddModal.story");
    expect(emx2Ids).toContain("/form/EditModal.story");
    expect(emx2Ids).toContain("/Session.story");
  });

  it("two-pass claiming: EMX2 claims Ref/RefBack/RefSelect/Ontology before Inputs' dir sweep", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    const inputsIds = sectionHeaderIds(sections, "Inputs");
    expect(emx2Ids).toContain("/input/Ref.story");
    expect(emx2Ids).toContain("/input/RefBack.story");
    expect(emx2Ids).toContain("/input/RefSelect.story");
    expect(emx2Ids).toContain("/input/Ontology.story");
    expect(inputsIds).not.toContain("/input/Ref.story");
    expect(inputsIds).not.toContain("/input/RefBack.story");
    expect(inputsIds).not.toContain("/input/RefSelect.story");
    expect(inputsIds).not.toContain("/input/Ontology.story");
  });

  it("two-pass: Ref is in EMX2 not Inputs even though EMX2 is the last group", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    const inputsIds = sectionHeaderIds(sections, "Inputs");
    expect(emx2Ids).toContain("/input/Ref.story");
    expect(inputsIds).not.toContain("/input/Ref.story");
  });

  it("EMX2 group contains both table stories", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    expect(emx2Ids).toContain("/table/EMX2.story");
    expect(emx2Ids).toContain("/table/modal/Ref.story");
  });

  it("Inputs only contains dumb/presentational inputs (no Ref/RefBack/RefSelect/Ontology/Switch)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const inputsIds = sectionHeaderIds(sections, "Inputs");
    expect(inputsIds).toContain("/input/String.story");
    expect(inputsIds).toContain("/input/Boolean.story");
    expect(inputsIds).toContain("/input/Array.story");
    expect(inputsIds).not.toContain("/input/Ref.story");
    expect(inputsIds).not.toContain("/input/RefBack.story");
    expect(inputsIds).not.toContain("/input/RefSelect.story");
    expect(inputsIds).not.toContain("/input/Ontology.story");
    expect(inputsIds).not.toContain("/input/Switch.story");
  });

  it("Feedback has Error and RequiredInfoSection (dissolved from Forms)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const feedbackIds = sectionHeaderIds(sections, "Feedback");
    expect(feedbackIds).toContain("/form/Error.story");
    expect(feedbackIds).toContain("/form/RequiredInfoSection.story");
  });

  it("Feedback has label/Draft via {dir:label}", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const feedbackIds = sectionHeaderIds(sections, "Feedback");
    expect(feedbackIds).toContain("/label/Draft.story");
  });

  it("Display has Image, Logo, LogoMobile, Icons AND Heading/Paragraph/page Banner (merged from dissolved Pages)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const displayIds = sectionHeaderIds(sections, "Display");
    expect(displayIds).toContain("/Image.story");
    expect(displayIds).toContain("/Logo.story");
    expect(displayIds).toContain("/LogoMobile.story");
    expect(displayIds).toContain("/Icons.story");
    expect(displayIds).toContain("/pages/Heading.story");
    expect(displayIds).toContain("/pages/Paragraph.story");
    expect(displayIds).toContain("/pages/Banner.story");
  });

  it("Pages group is dissolved — no Pages section exists", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    expect(findSection(sections, "Pages")).toBeUndefined();
  });

  it("pages/Banner.story is labeled 'Page banner' in Display headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const displaySection = findSection(sections, "Display");
    const pageBannerEntry = (displaySection?.headers ?? []).find(
      (header) => header.id === "/pages/Banner.story"
    );
    expect(pageBannerEntry).toBeDefined();
    expect(pageBannerEntry!.label).toBe("Page banner");
  });

  it("input/String.story.vue lands in Inputs headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const ids = sectionHeaderIds(sections, "Inputs");
    expect(ids).toContain("/input/String.story");
  });

  it("pages/NavigationGroups.story.vue lands in Navigation headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const ids = sectionHeaderIds(sections, "Navigation");
    expect(ids).toContain("/pages/NavigationGroups.story");
  });

  it("root Banner.story lands in Feedback; pages/Banner.story lands in Display (not Feedback)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const feedbackIds = sectionHeaderIds(sections, "Feedback");
    const displayIds = sectionHeaderIds(sections, "Display");
    expect(feedbackIds).toContain("/Banner.story");
    expect(feedbackIds).not.toContain("/pages/Banner.story");
    expect(displayIds).toContain("/pages/Banner.story");
    expect(displayIds).not.toContain("/Banner.story");
  });

  it("Prototypes is a container section with empty headers and overview route /section/prototypes", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const prototypes = findSection(sections, "Prototypes");
    expect(prototypes).toBeDefined();
    expect(prototypes?.id).toBe("/section/prototypes");
    expect(prototypes?.headers).toHaveLength(0);
  });

  it("Page templates does NOT contain a Patterns overview link", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const pageTemplatesIds = sectionHeaderIds(sections, "Page templates");
    expect(pageTemplatesIds).not.toContain("/patterns");
    expect(pageTemplatesIds).toContain("/samples/rowEdit");
    expect(pageTemplatesIds).toContain("/samples/formModal");
  });

  it("a story with an unknown dir falls into Ungrouped section", () => {
    const pathsWithUnknown = [
      ...allRealPaths,
      "../pages/unknowndir/Widget.story.vue",
    ];
    const sections = buildDocsLegend(pathsWithUnknown, "");
    const ungrouped = findSection(sections, "Ungrouped");
    expect(ungrouped).toBeDefined();
    const ids = (ungrouped?.headers ?? []).map((header) => header.id);
    expect(ids).toContain("/unknowndir/Widget.story");
  });

  it("a root-level story with no dir match falls into Ungrouped", () => {
    const pathsWithUnknown = [
      ...allRealPaths,
      "../pages/UnknownWidget.story.vue",
    ];
    const sections = buildDocsLegend(pathsWithUnknown, "");
    const ungrouped = findSection(sections, "Ungrouped");
    expect(ungrouped).toBeDefined();
    const ids = (ungrouped?.headers ?? []).map((header) => header.id);
    expect(ids).toContain("/UnknownWidget.story");
  });

  it("Foundations section has static links including Theme styles overview, Colors, and Theme switch", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const foundationIds = sectionHeaderIds(sections, "Foundations");
    expect(foundationIds).toContain("/Styles.other");
    expect(foundationIds).toContain("/foundations/colors");
    expect(foundationIds).toContain("/ThemeSwitch.story");
  });

  it("Get started is a link-section with id /get-started and no headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const getStarted = findSection(sections, "Get started");
    expect(getStarted).toBeDefined();
    expect(getStarted?.id).toBe("/get-started");
    expect(getStarted?.headers).toHaveLength(0);
  });

  it("Data fetching is a link-section with id /DataFetch.other and no headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const dataFetching = findSection(sections, "Data fetching");
    expect(dataFetching).toBeDefined();
    expect(dataFetching?.id).toBe("/DataFetch.other");
    expect(dataFetching?.headers).toHaveLength(0);
  });

  it("container sections with an overview route have id /section/<slug>", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    expect(findSection(sections, "Foundations")?.id).toBe(
      "/section/foundations"
    );
    expect(findSection(sections, "Actions")?.id).toBe("/section/actions");
    expect(findSection(sections, "Inputs")?.id).toBe("/section/inputs");
    expect(findSection(sections, "Navigation")?.id).toBe("/section/navigation");
    expect(findSection(sections, "Feedback")?.id).toBe("/section/feedback");
    expect(findSection(sections, "Overlays")?.id).toBe("/section/overlays");
    expect(findSection(sections, "Display")?.id).toBe("/section/display");
    expect(findSection(sections, "Layout")?.id).toBe("/section/layout");
    expect(findSection(sections, "Visualisation")?.id).toBe(
      "/section/visualisation"
    );
    expect(findSection(sections, "EMX2")?.id).toBe("/section/emx2");
    expect(findSection(sections, "Page templates")?.id).toBe(
      "/section/page-templates"
    );
    expect(findSection(sections, "Prototypes")?.id).toBe("/section/prototypes");
  });

  it("viz stories land in Visualisation headers", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const vizIds = sectionHeaderIds(sections, "Visualisation");
    expect(vizIds).toContain("/viz/ColumnChart.story");
    expect(vizIds).toContain("/viz/PieChart.story");
  });

  it("Display group contains DisplayList (merged from dissolved Data display)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const displayIds = sectionHeaderIds(sections, "Display");
    expect(displayIds).toContain("/DisplayList.story");
    const emx2Ids = sectionHeaderIds(sections, "EMX2");
    expect(emx2Ids).toContain("/table/EMX2.story");
  });

  it("table/EMX2 is labeled 'Table', table/modal/Ref is labeled 'Table ref', input/Ref stays 'Ref'", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const emx2Headers = findSection(sections, "EMX2")?.headers ?? [];
    const tableEntry = emx2Headers.find(
      (header) => header.id === "/table/EMX2.story"
    );
    expect(tableEntry).toBeDefined();
    expect(tableEntry!.label).toBe("Table");
    const tableRefEntry = emx2Headers.find(
      (header) => header.id === "/table/modal/Ref.story"
    );
    expect(tableRefEntry).toBeDefined();
    expect(tableRefEntry!.label).toBe("Table ref");
    const refEntry = emx2Headers.find(
      (header) => header.id === "/input/Ref.story"
    );
    expect(refEntry).toBeDefined();
    expect(refEntry!.label).toBe("Ref");
  });

  it("there is no 'Data display' group; its members are present in 'Display'", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    expect(findSection(sections, "Data display")).toBeUndefined();
    const displayIds = sectionHeaderIds(sections, "Display");
    expect(displayIds).toContain("/DisplayList.story");
    expect(displayIds).toContain("/Image.story");
    expect(displayIds).toContain("/Logo.story");
    expect(displayIds).toContain("/LogoMobile.story");
    expect(displayIds).toContain("/Icons.story");
    expect(displayIds).toContain("/text/Hyperlink.story");
  });

  it("each component group's headers are sorted alphabetically by label", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const groupLabels = [
      "Actions",
      "Inputs",
      "Navigation",
      "Feedback",
      "Overlays",
      "Display",
      "Layout",
      "Visualisation",
      "EMX2",
    ];
    for (const groupLabel of groupLabels) {
      const headers = findSection(sections, groupLabel)?.headers ?? [];
      const labels = headers.map((header) => header.label);
      const sorted = [...labels].sort((alpha, bravo) =>
        alpha.localeCompare(bravo)
      );
      expect(labels, `${groupLabel} headers should be alphabetical`).toEqual(
        sorted
      );
    }
  });

  it("Foundations headers are in curated order (Colors first, Theme switch last)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const foundationsHeaders =
      findSection(sections, "Foundations")?.headers ?? [];
    const labels = foundationsHeaders.map((header) => header.label);
    expect(labels[0]).toBe("Colors");
    expect(labels[1]).toBe("Typography");
    expect(labels[2]).toBe("Spacing");
    expect(labels[3]).toBe("Radius & elevation");
    expect(labels[4]).toBe("Icons");
    expect(labels[5]).toBe("Theme styles overview");
    expect(labels[6]).toBe("Theme switch");
  });

  it("Page templates headers are sorted alphabetically (Edit modal before Row edit)", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const headers = findSection(sections, "Page templates")?.headers ?? [];
    const labels = headers.map((header) => header.label);
    const sorted = [...labels].sort((alpha, bravo) =>
      alpha.localeCompare(bravo)
    );
    expect(labels).toEqual(sorted);
    expect(labels[0]).toBe("Edit modal");
    expect(labels[1]).toBe("Row edit");
  });

  it("Icons.story.vue is labeled 'Icon component' in Display headers, not 'Icons'", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    const displaySection = findSection(sections, "Display");
    const iconsEntry = (displaySection?.headers ?? []).find(
      (header) => header.id === "/Icons.story"
    );
    expect(iconsEntry).toBeDefined();
    expect(iconsEntry!.label).toBe("Icon component");
    expect(iconsEntry!.label).not.toBe("Icons");
  });

  it("section order is deterministic and matches config order regardless of input path order", () => {
    const shuffled = [...allRealPaths].reverse();
    const sectionsOriginal = buildDocsLegend(allRealPaths, "");
    const sectionsShuffled = buildDocsLegend(shuffled, "");
    const labelsOriginal = sectionsOriginal.map((section) => section.label);
    const labelsShuffled = sectionsShuffled.map((section) => section.label);
    expect(labelsOriginal).toEqual(labelsShuffled);
    expect(labelsOriginal[2]).toBe("Actions");
    expect(labelsOriginal[10]).toBe("EMX2");
  });

  it("within-group dir-matched headings are sorted alphabetically by label, not glob order", () => {
    const unordered = [
      "../pages/viz/PieChart.story.vue",
      "../pages/viz/ColumnChart.story.vue",
      "../pages/viz/ProgressMeter.story.vue",
      "../pages/viz/ChartLegend.story.vue",
    ];
    const reversed = [...unordered].reverse();
    const sectionsOriginal = buildDocsLegend(unordered, "");
    const sectionsReversed = buildDocsLegend(reversed, "");
    const vizHeadingsOriginal =
      findSection(sectionsOriginal, "Visualisation")?.headers ?? [];
    const vizHeadingsReversed =
      findSection(sectionsReversed, "Visualisation")?.headers ?? [];
    expect(vizHeadingsOriginal.map((header) => header.label)).toEqual(
      vizHeadingsReversed.map((header) => header.label)
    );
    const labels = vizHeadingsOriginal.map((header) => header.label);
    const sorted = [...labels].sort((alpha, bravo) =>
      alpha.localeCompare(bravo)
    );
    expect(labels).toEqual(sorted);
  });

  it("a story with no config match lands in Ungrouped, not silently dropped", () => {
    const pathsWithOrphan = [
      ...allRealPaths,
      "../pages/brandnewdir/OrphanWidget.story.vue",
    ];
    const sections = buildDocsLegend(pathsWithOrphan, "");
    const ungrouped = findSection(sections, "Ungrouped");
    expect(ungrouped).toBeDefined();
    const ids = (ungrouped?.headers ?? []).map((header) => header.id);
    expect(ids).toContain("/brandnewdir/OrphanWidget.story");
  });

  it("Ungrouped section has id empty string and no overview route", () => {
    const pathsWithOrphan = [
      ...allRealPaths,
      "../pages/unknowndir/Orphan.story.vue",
    ];
    const sections = buildDocsLegend(pathsWithOrphan, "");
    const ungrouped = findSection(sections, "Ungrouped");
    expect(ungrouped?.id).toBe("");
  });

  it("pages/Banner.story lands in Display not Feedback (red-green guard)", () => {
    const pathsWithPagesBanner = [
      "../pages/Banner.story.vue",
      "../pages/pages/Banner.story.vue",
    ];
    const sections = buildDocsLegend(pathsWithPagesBanner, "");
    const feedbackIds = sectionHeaderIds(sections, "Feedback");
    const displayIds = sectionHeaderIds(sections, "Display");
    expect(feedbackIds).toContain("/Banner.story");
    expect(feedbackIds).not.toContain("/pages/Banner.story");
    expect(displayIds).toContain("/pages/Banner.story");
  });

  it("isActive is a plain boolean on all sections and headings", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      expect(typeof section.isActive).toBe("boolean");
      for (const header of section.headers) {
        expect(typeof header.isActive).toBe("boolean");
      }
    }
  });

  it("isActive is false on all items when currentPath is empty string", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      expect(section.isActive).toBe(false);
      for (const header of section.headers) {
        expect(header.isActive).toBe(false);
      }
    }
  });

  it("isActive is true only on the matching link-section when currentPath is /get-started", () => {
    const sections = buildDocsLegend(allRealPaths, "/get-started");
    const getStarted = findSection(sections, "Get started");
    expect(getStarted?.isActive).toBe(true);
    const dataFetching = findSection(sections, "Data fetching");
    expect(dataFetching?.isActive).toBe(false);
    const foundations = findSection(sections, "Foundations");
    expect(foundations?.isActive).toBe(false);
  });

  it("isActive is true on both a heading and its containing section when on a component route", () => {
    const sections = buildDocsLegend(allRealPaths, "/viz/ColumnChart.story");
    const vizSection = findSection(sections, "Visualisation");
    expect(vizSection?.isActive).toBe(true);
    const columnChart = (vizSection?.headers ?? []).find(
      (header) => header.id === "/viz/ColumnChart.story"
    );
    expect(columnChart?.isActive).toBe(true);
    const otherVizHeadings = (vizSection?.headers ?? []).filter(
      (header) => header.id !== "/viz/ColumnChart.story"
    );
    for (const heading of otherVizHeadings) {
      expect(heading.isActive).toBe(false);
    }
    const actionsSection = findSection(sections, "Actions");
    expect(actionsSection?.isActive).toBe(false);
  });

  it("a container section is active when currentPath is its overview route", () => {
    const sections = buildDocsLegend(allRealPaths, "/section/visualisation");
    const vizSection = findSection(sections, "Visualisation");
    expect(vizSection?.isActive).toBe(true);
    for (const header of vizSection?.headers ?? []) {
      expect(header.isActive).toBe(false);
    }
    const actionsSection = findSection(sections, "Actions");
    expect(actionsSection?.isActive).toBe(false);
  });

  it("EMX2 section is active when currentPath is /section/emx2", () => {
    const sections = buildDocsLegend(allRealPaths, "/section/emx2");
    const emx2Section = findSection(sections, "EMX2");
    expect(emx2Section?.isActive).toBe(true);
    for (const header of emx2Section?.headers ?? []) {
      expect(header.isActive).toBe(false);
    }
    const actionsSection = findSection(sections, "Actions");
    expect(actionsSection?.isActive).toBe(false);
  });

  it("EMX2 section is active when on a smart-input route (Ref)", () => {
    const sections = buildDocsLegend(allRealPaths, "/input/Ref.story");
    const emx2Section = findSection(sections, "EMX2");
    expect(emx2Section?.isActive).toBe(true);
    const inputsSection = findSection(sections, "Inputs");
    expect(inputsSection?.isActive).toBe(false);
  });

  it("Prototypes section is active when currentPath is /section/prototypes", () => {
    const sections = buildDocsLegend(allRealPaths, "/section/prototypes");
    const prototypes = findSection(sections, "Prototypes");
    expect(prototypes?.isActive).toBe(true);
    const actions = findSection(sections, "Actions");
    expect(actions?.isActive).toBe(false);
  });

  it("only the container section with the overview route or active heading is active", () => {
    const sections = buildDocsLegend(allRealPaths, "/section/foundations");
    const foundations = findSection(sections, "Foundations");
    expect(foundations?.isActive).toBe(true);
    const nonFoundations = sections.filter(
      (section) => section.label !== "Foundations"
    );
    for (const section of nonFoundations) {
      expect(section.isActive, `${section.label} should not be active`).toBe(
        false
      );
    }
  });

  it("errorCount is always 0 for all sections and headings", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      expect(section.errorCount.value).toBe(0);
      for (const header of section.headers) {
        expect(header.errorCount.value).toBe(0);
      }
    }
  });

  it("isVisible is always true for all sections and headings", () => {
    const sections = buildDocsLegend(allRealPaths, "");
    for (const section of sections) {
      expect(section.isVisible.value).toBe(true);
      for (const header of section.headers) {
        expect(header.isVisible.value).toBe(true);
      }
    }
  });
});

describe("getSectionOverview", () => {
  it("returns title, description, and items for emx2 slug", () => {
    const result = getSectionOverview("emx2", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.slug).toBe("emx2");
    expect(result?.title).toBe("EMX2");
    expect(typeof result?.description).toBe("string");
    expect(result!.description.length).toBeGreaterThan(0);
    expect(
      result?.items.some((item) => item.route === "/input/Ref.story")
    ).toBe(true);
    expect(result?.items.some((item) => item.route === "/Session.story")).toBe(
      true
    );
    expect(
      result?.items.some((item) => item.route === "/table/EMX2.story")
    ).toBe(true);
  });

  it("returns null for the dissolved forms slug", () => {
    expect(getSectionOverview("forms", allRealPaths)).toBeNull();
  });

  it("returns title, description, and items for visualisation slug", () => {
    const result = getSectionOverview("visualisation", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.slug).toBe("visualisation");
    expect(result?.title).toBe("Visualisation");
    expect(typeof result?.description).toBe("string");
    expect(result!.description.length).toBeGreaterThan(0);
    expect(
      result?.items.some((item) => item.route === "/viz/ColumnChart.story")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/viz/PieChart.story")
    ).toBe(true);
  });

  it("returns null for an unknown slug", () => {
    const result = getSectionOverview("nonexistent-slug", allRealPaths);
    expect(result).toBeNull();
  });

  it("returns null for old slugs that have been replaced", () => {
    expect(getSectionOverview("data-visualisation", allRealPaths)).toBeNull();
    expect(getSectionOverview("content-media", allRealPaths)).toBeNull();
    expect(getSectionOverview("visualization", allRealPaths)).toBeNull();
    expect(getSectionOverview("data-display", allRealPaths)).toBeNull();
  });

  it("display overview includes merged members: DisplayList and text/Hyperlink", () => {
    const result = getSectionOverview("display", allRealPaths);
    expect(result).not.toBeNull();
    expect(
      result?.items.some((item) => item.route === "/DisplayList.story")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/text/Hyperlink.story")
    ).toBe(true);
  });

  it("Foundations overview includes foundations link items and Theme switch", () => {
    const result = getSectionOverview("foundations", allRealPaths);
    expect(result).not.toBeNull();
    expect(
      result?.items.some((item) => item.route === "/foundations/colors")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/foundations/typography")
    ).toBe(true);
    expect(result?.items.some((item) => item.route === "/Styles.other")).toBe(
      true
    );
    expect(
      result?.items.some((item) => item.route === "/ThemeSwitch.story")
    ).toBe(true);
  });

  it("page-templates overview includes row-edit and edit-modal but NOT patterns overview", () => {
    const result = getSectionOverview("page-templates", allRealPaths);
    expect(result).not.toBeNull();
    expect(
      result?.items.some((item) => item.route === "/samples/rowEdit")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/samples/formModal")
    ).toBe(true);
    expect(result?.items.some((item) => item.route === "/patterns")).toBe(
      false
    );
  });

  it("prototypes overview has empty items list", () => {
    const result = getSectionOverview("prototypes", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.slug).toBe("prototypes");
    expect(result?.title).toBe("Prototypes");
    expect(result?.items).toHaveLength(0);
  });

  it("feedback overview includes Banner, Message, Error, RequiredInfoSection, and Draft", () => {
    const result = getSectionOverview("feedback", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.items.some((item) => item.route === "/Banner.story")).toBe(
      true
    );
    expect(result?.items.some((item) => item.route === "/Message.story")).toBe(
      true
    );
    expect(
      result?.items.some((item) => item.route === "/form/Error.story")
    ).toBe(true);
    expect(
      result?.items.some(
        (item) => item.route === "/form/RequiredInfoSection.story"
      )
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/label/Draft.story")
    ).toBe(true);
  });

  it("navigation overview includes Legend", () => {
    const result = getSectionOverview("navigation", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.items.some((item) => item.route === "/Legend.story")).toBe(
      true
    );
  });

  it("overlays overview includes Modal and SideModal", () => {
    const result = getSectionOverview("overlays", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.items.some((item) => item.route === "/Modal.story")).toBe(
      true
    );
    expect(
      result?.items.some((item) => item.route === "/SideModal.story")
    ).toBe(true);
  });

  it("display overview includes Image, Icons, Heading, Paragraph, and page Banner (merged from dissolved Pages)", () => {
    const result = getSectionOverview("display", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.items.some((item) => item.route === "/Image.story")).toBe(
      true
    );
    expect(result?.items.some((item) => item.route === "/Icons.story")).toBe(
      true
    );
    expect(
      result?.items.some((item) => item.route === "/pages/Heading.story")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/pages/Paragraph.story")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/pages/Banner.story")
    ).toBe(true);
  });

  it("pages slug returns null (Pages group is dissolved into Display)", () => {
    expect(getSectionOverview("pages", allRealPaths)).toBeNull();
  });

  it("items list is non-empty for all known slugs (except prototypes)", () => {
    const slugsWithItems = [
      "foundations",
      "actions",
      "inputs",
      "navigation",
      "feedback",
      "overlays",
      "display",
      "layout",
      "visualisation",
      "emx2",
      "page-templates",
    ];
    for (const slug of slugsWithItems) {
      const result = getSectionOverview(slug, allRealPaths);
      expect(result, `${slug} should return a result`).not.toBeNull();
      expect(
        result!.items.length,
        `${slug} should have non-empty items`
      ).toBeGreaterThan(0);
    }
  });

  it("Actions overview includes Button, ButtonBar, and Switch", () => {
    const result = getSectionOverview("actions", allRealPaths);
    expect(result?.items.some((item) => item.label === "Button")).toBe(true);
    expect(result?.items.some((item) => item.label === "ButtonBar")).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/input/Switch.story")
    ).toBe(true);
  });

  it("Display overview has Icons labeled as 'Icon component'", () => {
    const result = getSectionOverview("display", allRealPaths);
    const iconsItem = result?.items.find(
      (item) => item.route === "/Icons.story"
    );
    expect(iconsItem).toBeDefined();
    expect(iconsItem?.label).toBe("Icon component");
  });
});

describe("getSectionNavForRoute", () => {
  it("returns EMX2 section for a smart-input route", () => {
    const result = getSectionNavForRoute("/input/Ref.story", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.title).toBe("EMX2");
    expect(result?.slug).toBe("emx2");
    expect(result?.overviewRoute).toBe("/section/emx2");
    expect(
      result?.items.some((item) => item.route === "/input/Ref.story")
    ).toBe(true);
  });

  it("returns EMX2 section when on /section/emx2 overview page", () => {
    const result = getSectionNavForRoute("/section/emx2", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.title).toBe("EMX2");
    expect(result?.slug).toBe("emx2");
    expect(result?.items.some((item) => item.route === "/Session.story")).toBe(
      true
    );
  });

  it("returns section info for a component route", () => {
    const result = getSectionNavForRoute(
      "/viz/ColumnChart.story",
      allRealPaths
    );
    expect(result).not.toBeNull();
    expect(result?.title).toBe("Visualisation");
    expect(result?.slug).toBe("visualisation");
    expect(result?.overviewRoute).toBe("/section/visualisation");
    expect(
      result?.items.some((item) => item.route === "/viz/ColumnChart.story")
    ).toBe(true);
    expect(
      result?.items.some((item) => item.route === "/viz/PieChart.story")
    ).toBe(true);
  });

  it("returns section info when currentRoute is a /section/<slug> overview page", () => {
    const result = getSectionNavForRoute("/section/actions", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.title).toBe("Actions");
    expect(result?.slug).toBe("actions");
    expect(result?.overviewRoute).toBe("/section/actions");
    expect(result?.items.some((item) => item.label === "Button")).toBe(true);
  });

  it("returns null for /get-started (link-section, no items)", () => {
    const result = getSectionNavForRoute("/get-started", allRealPaths);
    expect(result).toBeNull();
  });

  it("returns null for /DataFetch.other (link-section, no items)", () => {
    const result = getSectionNavForRoute("/DataFetch.other", allRealPaths);
    expect(result).toBeNull();
  });

  it("returns section info for a foundations static-link route", () => {
    const result = getSectionNavForRoute("/foundations/colors", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.title).toBe("Foundations");
    expect(result?.slug).toBe("foundations");
    expect(
      result?.items.some((item) => item.route === "/foundations/colors")
    ).toBe(true);
  });

  it("returns Prototypes with empty items when on /section/prototypes", () => {
    const result = getSectionNavForRoute("/section/prototypes", allRealPaths);
    expect(result).not.toBeNull();
    expect(result?.title).toBe("Prototypes");
    expect(result?.slug).toBe("prototypes");
    expect(result?.items).toHaveLength(0);
  });

  it("returns Actions section when on Switch route", () => {
    const result = getSectionNavForRoute("/input/Switch.story", allRealPaths);
    expect(result?.title).toBe("Actions");
    expect(
      result?.items.some((item) => item.route === "/input/Switch.story")
    ).toBe(true);
  });

  it("returns null for an unknown route", () => {
    const result = getSectionNavForRoute("/unknown/route", allRealPaths);
    expect(result).toBeNull();
  });
});

describe("buildDocsSidebar", () => {
  it("returns the same number of sections as buildDocsLegend", () => {
    const legend = buildDocsLegend(allRealPaths, "");
    const sidebar = buildDocsSidebar(allRealPaths, "", null);
    expect(sidebar).toHaveLength(legend.length);
  });

  it("expanded section retains its headers; all other sections have empty headers", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "", "foundations");
    const foundations = sidebar.find(
      (section) => section.label === "Foundations"
    );
    expect(foundations!.headers.length).toBeGreaterThan(0);
    for (const section of sidebar.filter((s) => s.label !== "Foundations")) {
      expect(
        section.headers,
        `${section.label} should have empty headers`
      ).toHaveLength(0);
    }
  });

  it("all sections have empty headers when expandedSlug is null", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "", null);
    for (const section of sidebar) {
      expect(section.headers).toHaveLength(0);
    }
  });

  it("section labels and ids match buildDocsLegend output", () => {
    const legend = buildDocsLegend(allRealPaths, "");
    const sidebar = buildDocsSidebar(allRealPaths, "", null);
    for (let index = 0; index < legend.length; index++) {
      expect(sidebar[index]!.label).toBe(legend[index]!.label);
      expect(sidebar[index]!.id).toBe(legend[index]!.id);
    }
  });

  it("EMX2 section is present in sidebar with id /section/emx2 and empty headers", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "", null);
    const emx2 = sidebar.find((section) => section.label === "EMX2");
    expect(emx2).toBeDefined();
    expect(emx2?.id).toBe("/section/emx2");
    expect(emx2?.headers).toHaveLength(0);
  });

  it("isActive is true for the section containing the current component route", () => {
    const sidebar = buildDocsSidebar(
      allRealPaths,
      "/viz/ColumnChart.story",
      null
    );
    const vizSection = sidebar.find(
      (section) => section.label === "Visualisation"
    );
    expect(vizSection?.isActive).toBe(true);
    const actionsSection = sidebar.find(
      (section) => section.label === "Actions"
    );
    expect(actionsSection?.isActive).toBe(false);
  });

  it("isActive is true for EMX2 when on a smart-input route", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "/input/Ref.story", null);
    const emx2 = sidebar.find((section) => section.label === "EMX2");
    expect(emx2?.isActive).toBe(true);
    const inputs = sidebar.find((section) => section.label === "Inputs");
    expect(inputs?.isActive).toBe(false);
  });

  it("isActive is true for a section when on its overview page", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "/section/actions", null);
    const actionsSection = sidebar.find(
      (section) => section.label === "Actions"
    );
    expect(actionsSection?.isActive).toBe(true);
    const inputsSection = sidebar.find((section) => section.label === "Inputs");
    expect(inputsSection?.isActive).toBe(false);
  });

  it("isActive is true for EMX2 when on /section/emx2 overview", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "/section/emx2", null);
    const emx2 = sidebar.find((section) => section.label === "EMX2");
    expect(emx2?.isActive).toBe(true);
  });

  it("isActive is true for Get started link-section when on /get-started", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "/get-started", null);
    const getStarted = sidebar.find(
      (section) => section.label === "Get started"
    );
    expect(getStarted?.isActive).toBe(true);
    const grouped = sidebar.filter(
      (section) => section.label !== "Get started"
    );
    for (const section of grouped) {
      expect(section.isActive, `${section.label} should not be active`).toBe(
        false
      );
    }
  });

  it("expansion follows expandedSlug not currentPath: Foundations expanded when on an Actions route", () => {
    const sidebar = buildDocsSidebar(
      allRealPaths,
      "/Button.story",
      "foundations"
    );
    const foundations = sidebar.find(
      (section) => section.label === "Foundations"
    );
    const actions = sidebar.find((section) => section.label === "Actions");
    expect(foundations!.headers.length).toBeGreaterThan(0);
    expect(actions?.headers).toHaveLength(0);
    expect(actions?.isActive).toBe(true);
    expect(foundations?.isActive).toBe(false);
  });

  it("isActive is still route-driven when expandedSlug differs from active section", () => {
    const sidebar = buildDocsSidebar(
      allRealPaths,
      "/viz/ColumnChart.story",
      "foundations"
    );
    const visualisation = sidebar.find(
      (section) => section.label === "Visualisation"
    );
    const foundations = sidebar.find(
      (section) => section.label === "Foundations"
    );
    expect(visualisation?.isActive).toBe(true);
    expect(visualisation?.headers).toHaveLength(0);
    expect(foundations?.isActive).toBe(false);
    expect(foundations!.headers.length).toBeGreaterThan(0);
  });
});

describe("buildDocsSidebar search", () => {
  it("(a) query 'ref' returns only sections with matching pages; non-matching pages filtered out", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "", null, "ref");
    const emx2 = sidebar.find((section) => section.label === "EMX2");
    expect(emx2).toBeDefined();
    expect(emx2!.headers.some((header) => header.label === "Ref")).toBe(true);
    expect(emx2!.headers.some((header) => header.label === "RefBack")).toBe(
      true
    );
    expect(emx2!.headers.some((header) => header.label === "RefSelect")).toBe(
      true
    );
    const actions = sidebar.find((section) => section.label === "Actions");
    expect(actions).toBeUndefined();
    for (const section of sidebar) {
      for (const header of section.headers) {
        expect(
          header.label,
          `header "${header.label}" should match "ref"`
        ).toMatch(/ref/i);
      }
    }
  });

  it("(b) multi-word AND: both words must match — 'column chart' narrows to only ColumnChart", () => {
    const sidebar = buildDocsSidebar(allRealPaths, "", null, "column chart");
    const allHeaders = sidebar.flatMap((section) => section.headers);
    expect(allHeaders.some((header) => header.label === "ColumnChart")).toBe(
      true
    );
    expect(
      allHeaders.every(
        (header) => /column/i.test(header.label) && /chart/i.test(header.label)
      )
    ).toBe(true);
    const actions = sidebar.find((section) => section.label === "Actions");
    expect(actions).toBeUndefined();
    const inputs = sidebar.find((section) => section.label === "Inputs");
    expect(inputs).toBeUndefined();
  });

  it("(c) query matching a SECTION name shows that section with all its pages", () => {
    const legend = buildDocsLegend(allRealPaths, "");
    const vizFull = legend.find((section) => section.label === "Visualisation");
    const sidebar = buildDocsSidebar(allRealPaths, "", null, "Visualisation");
    const viz = sidebar.find((section) => section.label === "Visualisation");
    expect(viz).toBeDefined();
    expect(viz!.headers).toHaveLength(vizFull!.headers.length);
    expect(viz!.headers.length).toBeGreaterThan(0);
  });

  it("(d) empty query returns all sections unchanged (accordion mode, all collapsed)", () => {
    const sidebarEmpty = buildDocsSidebar(allRealPaths, "", null, "");
    const sidebarDefault = buildDocsSidebar(allRealPaths, "", null);
    expect(sidebarEmpty).toHaveLength(sidebarDefault.length);
    for (let index = 0; index < sidebarEmpty.length; index++) {
      expect(sidebarEmpty[index]!.label).toBe(sidebarDefault[index]!.label);
      expect(sidebarEmpty[index]!.id).toBe(sidebarDefault[index]!.id);
      expect(sidebarEmpty[index]!.headers).toHaveLength(0);
    }
  });

  it("(e) invalid-regex word '(' does not throw and falls back to literal match", () => {
    expect(() => buildDocsSidebar(allRealPaths, "", null, "(")).not.toThrow();
    const sidebar = buildDocsSidebar(allRealPaths, "", null, "(");
    expect(Array.isArray(sidebar)).toBe(true);
    expect(sidebar.every((section) => Array.isArray(section.headers))).toBe(
      true
    );
  });
});

describe("getSectionTitleBySlug", () => {
  it("returns the human title for known slugs", () => {
    expect(getSectionTitleBySlug("foundations")).toBe("Foundations");
    expect(getSectionTitleBySlug("inputs")).toBe("Inputs");
    expect(getSectionTitleBySlug("feedback")).toBe("Feedback");
    expect(getSectionTitleBySlug("overlays")).toBe("Overlays");
    expect(getSectionTitleBySlug("display")).toBe("Display");
    expect(getSectionTitleBySlug("layout")).toBe("Layout");
    expect(getSectionTitleBySlug("visualisation")).toBe("Visualisation");
    expect(getSectionTitleBySlug("emx2")).toBe("EMX2");
    expect(getSectionTitleBySlug("page-templates")).toBe("Page templates");
    expect(getSectionTitleBySlug("prototypes")).toBe("Prototypes");
  });

  it("returns null for dissolved or unknown slugs", () => {
    expect(getSectionTitleBySlug("forms")).toBeNull();
    expect(getSectionTitleBySlug("pages")).toBeNull();
    expect(getSectionTitleBySlug("data-display")).toBeNull();
    expect(getSectionTitleBySlug("unknown")).toBeNull();
    expect(getSectionTitleBySlug("feedback-status")).toBeNull();
    expect(getSectionTitleBySlug("visualization")).toBeNull();
    expect(getSectionTitleBySlug("data-visualisation")).toBeNull();
    expect(getSectionTitleBySlug("patterns")).toBeNull();
    expect(getSectionTitleBySlug("content")).toBeNull();
    expect(getSectionTitleBySlug("content-media")).toBeNull();
  });
});
