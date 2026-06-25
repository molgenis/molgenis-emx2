import { computed } from "vue";
import type {
  LegendSection,
  LegendHeading,
} from "../../../metadata-utils/src/types";

type ExplicitMember = { storyName: string; route?: string };
type DirMember = { dir: string };
type GroupMember = ExplicitMember | DirMember;

type ComponentGroupConfig = {
  title: string;
  members: GroupMember[];
};

function isDirMember(member: GroupMember): member is DirMember {
  return "dir" in member;
}

const STORY_NAME_LABEL_OVERRIDE: Record<string, string> = {
  Icons: "Icon component",
};

const ROUTE_LABEL_OVERRIDE: Record<string, string> = {
  "/table/EMX2.story": "Table",
  "/table/modal/Ref.story": "Table ref",
};

type StaticLink = { label: string; route: string };

const FOUNDATIONS_LINKS: StaticLink[] = [
  { label: "Colors", route: "/foundations/colors" },
  { label: "Typography", route: "/foundations/typography" },
  { label: "Spacing", route: "/foundations/spacing" },
  { label: "Radius & elevation", route: "/foundations/radius-elevation" },
  { label: "Icons", route: "/foundations/icons" },
  { label: "Theme styles overview", route: "/Styles.other" },
  { label: "Theme switch", route: "/ThemeSwitch.story" },
];

const PATTERNS_LINKS: StaticLink[] = [
  { label: "Row edit", route: "/samples/rowEdit" },
  { label: "Edit modal", route: "/samples/formModal" },
];

export type SectionOverviewConfig = {
  slug: string;
  title: string;
  description: string;
};

const SECTION_OVERVIEW_CONFIG: SectionOverviewConfig[] = [
  {
    slug: "foundations",
    title: "Foundations",
    description:
      "Design tokens — colors, typography, spacing, radius, elevation, and icon resources that underpin every component.",
  },
  {
    slug: "actions",
    title: "Actions",
    description:
      "Buttons, toggles, and interactive controls that trigger operations or navigate the user through a flow.",
  },
  {
    slug: "inputs",
    title: "Inputs",
    description:
      "Basic, presentation-only form controls — text fields, checkboxes, date pickers, and selects with no backend coupling.",
  },
  {
    slug: "navigation",
    title: "Navigation",
    description:
      "Components for moving between pages and sections — navbars, breadcrumbs, pagination, and the sectioned legend.",
  },
  {
    slug: "feedback",
    title: "Feedback",
    description:
      "Alert banners, status messages, form validation helpers, and in-page notifications that communicate system state.",
  },
  {
    slug: "overlays",
    title: "Overlays",
    description:
      "Modals, side panels, and tooltips that layer content above the page without full navigation.",
  },
  {
    slug: "display",
    title: "Display",
    description:
      "Media and brand elements (images, logos, icon rendering) plus data presentation renderers — display lists, value cells, and text utilities for showing structured data.",
  },
  {
    slug: "pages",
    title: "Pages",
    description:
      "Page-level content blocks for headings, paragraphs, and banner sections used inside content pages.",
  },
  {
    slug: "layout",
    title: "Layout",
    description:
      "Structural and chrome components — page header, footer, accordions, progressive reveals, and animated transitions.",
  },
  {
    slug: "visualisation",
    title: "Visualisation",
    description:
      "Charts and meters for presenting quantitative data — bar charts, pie charts, and progress indicators.",
  },
  {
    slug: "emx2",
    title: "EMX2",
    description:
      "Smart components that integrate with EMX2 schema metadata and backend APIs — forms, ref inputs, table renderers, and session.",
  },
  {
    slug: "page-templates",
    title: "Page templates",
    description:
      "Multi-component workflows and interaction patterns demonstrating how components combine in complete scenarios.",
  },
  {
    slug: "prototypes",
    title: "Prototypes",
    description:
      "Work-in-progress showcases for features under development before they become production components.",
  },
];

export function getSectionTitleBySlug(slug: string): string | null {
  return (
    SECTION_OVERVIEW_CONFIG.find((config) => config.slug === slug)?.title ??
    null
  );
}

export type SectionOverview = {
  slug: string;
  title: string;
  description: string;
  items: { label: string; route: string }[];
};

export function getSectionOverview(
  slug: string,
  storyModulePaths: string[]
): SectionOverview | null {
  const config = SECTION_OVERVIEW_CONFIG.find((c) => c.slug === slug);
  if (!config) return null;
  const overviewRoute = `/section/${slug}`;
  const sections = buildDocsLegend(storyModulePaths, "");
  const section = sections.find((sec) => sec.id === overviewRoute);
  const items = (section?.headers ?? []).map((header) => ({
    label: header.label,
    route: header.id,
  }));
  return {
    slug: config.slug,
    title: config.title,
    description: config.description,
    items,
  };
}

export type SectionNav = {
  title: string;
  slug: string;
  overviewRoute: string;
  items: { label: string; route: string }[];
};

export function getSectionNavForRoute(
  currentRoute: string,
  storyModulePaths: string[]
): SectionNav | null {
  const sections = buildDocsLegend(storyModulePaths, currentRoute);
  const found = sections.find((section) => {
    if (section.headers.some((header) => header.id === currentRoute))
      return true;
    if (section.id === currentRoute && section.id.startsWith("/section/"))
      return true;
    return false;
  });
  if (!found) return null;
  const overviewRoute = found.id;
  const slug = overviewRoute.startsWith("/section/")
    ? overviewRoute.slice("/section/".length)
    : overviewRoute;
  return {
    title: found.label,
    slug,
    overviewRoute,
    items: found.headers.map((header) => ({
      label: header.label,
      route: header.id,
    })),
  };
}

function escapeRegExp(word: string): string {
  return word.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function buildWordRegexps(query: string): RegExp[] {
  return query
    .trim()
    .split(/\s+/)
    .map((word) => {
      try {
        return new RegExp(word, "i");
      } catch {
        return new RegExp(escapeRegExp(word), "i");
      }
    });
}

function matchesAllRegexps(text: string, regexps: RegExp[]): boolean {
  return regexps.every((regexp) => regexp.test(text));
}

export function buildDocsSidebar(
  storyModulePaths: string[],
  currentPath: string,
  expandedSlug: string | null,
  query: string = ""
): LegendSection[] {
  const allSections = buildDocsLegend(storyModulePaths, currentPath);

  if (query.trim() === "") {
    return allSections.map((section) => ({
      ...section,
      headers:
        expandedSlug !== null && section.id === `/section/${expandedSlug}`
          ? section.headers
          : [],
    }));
  }

  const regexps = buildWordRegexps(query);
  const filtered: LegendSection[] = [];

  for (const section of allSections) {
    const sectionMatches = matchesAllRegexps(section.label, regexps);
    const matchingHeaders = section.headers.filter((header) =>
      matchesAllRegexps(header.label, regexps)
    );
    if (sectionMatches || matchingHeaders.length > 0) {
      filtered.push({
        ...section,
        headers: sectionMatches ? section.headers : matchingHeaders,
      });
    }
  }

  return filtered;
}

const COMPONENT_GROUPS_CONFIG: ComponentGroupConfig[] = [
  {
    title: "Actions",
    members: [
      { storyName: "Button" },
      { storyName: "ButtonBar" },
      { storyName: "ButtonDropdown" },
      { storyName: "Switch", route: "/input/Switch.story" },
    ],
  },
  {
    title: "Inputs",
    members: [{ storyName: "FilterSearch" }, { dir: "input" }],
  },
  {
    title: "Navigation",
    members: [
      { storyName: "Navigation" },
      { storyName: "NavigationNested" },
      { storyName: "BreadCrumbs" },
      { storyName: "Pagination" },
      { storyName: "Header" },
      { storyName: "NavigationGroups", route: "/pages/NavigationGroups.story" },
      { storyName: "Legend" },
    ],
  },
  {
    title: "Feedback",
    members: [
      { storyName: "Banner" },
      { storyName: "Message" },
      { storyName: "Error", route: "/form/Error.story" },
      {
        storyName: "RequiredInfoSection",
        route: "/form/RequiredInfoSection.story",
      },
      { dir: "notification" },
      { dir: "label" },
    ],
  },
  {
    title: "Overlays",
    members: [
      { storyName: "Modal" },
      { storyName: "SideModal" },
      { storyName: "CustomTooltip" },
    ],
  },
  {
    title: "Display",
    members: [
      { storyName: "Image" },
      { storyName: "Logo" },
      { storyName: "LogoMobile" },
      { storyName: "Icons" },
      { storyName: "DisplayList" },
      { dir: "display" },
      { dir: "value" },
      { dir: "text" },
    ],
  },
  {
    title: "Pages",
    members: [
      { storyName: "Heading", route: "/pages/Heading.story" },
      { storyName: "Paragraph", route: "/pages/Paragraph.story" },
      { storyName: "Banner", route: "/pages/Banner.story" },
    ],
  },
  {
    title: "Layout",
    members: [
      { storyName: "Accordion" },
      { storyName: "ShowMore" },
      { dir: "transition" },
      { storyName: "PageHeader" },
      { storyName: "FooterComponent" },
    ],
  },
  {
    title: "Visualisation",
    members: [{ dir: "viz" }],
  },
  {
    title: "EMX2",
    members: [
      { storyName: "Form" },
      { storyName: "Field" },
      { storyName: "AddModal", route: "/form/AddModal.story" },
      { storyName: "EditModal", route: "/form/EditModal.story" },
      { storyName: "Ref", route: "/input/Ref.story" },
      { storyName: "RefBack", route: "/input/RefBack.story" },
      { storyName: "RefSelect", route: "/input/RefSelect.story" },
      { storyName: "Ontology", route: "/input/Ontology.story" },
      { dir: "table" },
      { storyName: "Session" },
    ],
  },
];

type ParsedStory = {
  route: string;
  label: string;
  topDir: string | null;
  storyName: string;
  isPagesSubdir: boolean;
};

function parseStoryPath(modulePath: string): ParsedStory {
  const withoutPrefix = modulePath.replace("../pages/", "/");
  const route = withoutPrefix.replace(".vue", "");
  const segments = withoutPrefix.split("/").filter(Boolean);
  const fileName = segments[segments.length - 1] ?? "";
  const storyName = fileName.replace(".story.vue", "");
  const firstSegment = segments[0] ?? null;
  const topDir: string | null = segments.length > 1 ? firstSegment : null;
  const isPagesSubdir = topDir === "pages";
  const label =
    ROUTE_LABEL_OVERRIDE[route] ??
    STORY_NAME_LABEL_OVERRIDE[storyName] ??
    storyName;
  return { route, label, topDir, storyName, isPagesSubdir };
}

function storyMatchesMember(parsed: ParsedStory, member: GroupMember): boolean {
  if (isDirMember(member)) {
    if (parsed.isPagesSubdir) return false;
    return parsed.topDir === member.dir;
  }
  if (member.route !== undefined) {
    return parsed.route === member.route;
  }
  if (parsed.isPagesSubdir) {
    return false;
  }
  if (parsed.topDir === null) {
    return parsed.storyName === member.storyName;
  }
  return false;
}

function makeHeading(
  route: string,
  label: string,
  currentPath: string
): LegendHeading {
  return {
    id: route,
    label,
    type: "HEADING",
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: currentPath === route,
  };
}

function makeContainerSection(
  label: string,
  headers: LegendHeading[],
  currentPath: string,
  overviewRoute: string | null
): LegendSection {
  return {
    id: overviewRoute ?? "",
    label,
    type: "SECTION",
    headers,
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive:
      (overviewRoute !== null && currentPath === overviewRoute) ||
      headers.some((header) => header.id === currentPath),
  };
}

function makeLinkSection(
  route: string,
  label: string,
  currentPath: string
): LegendSection {
  return {
    id: route,
    label,
    type: "SECTION",
    headers: [],
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: currentPath === route,
  };
}

function parsedToHeading(
  parsed: ParsedStory,
  currentPath: string
): LegendHeading {
  return makeHeading(parsed.route, parsed.label, currentPath);
}

function staticLinksToHeadings(
  links: StaticLink[],
  currentPath: string
): LegendHeading[] {
  return links.map((link) => makeHeading(link.route, link.label, currentPath));
}

export function buildDocsLegend(
  storyModulePaths: string[],
  currentPath: string
): LegendSection[] {
  const allParsed = storyModulePaths.map(parseStoryPath);
  const claimedRoutes = new Set<string>();

  for (const link of FOUNDATIONS_LINKS) {
    claimedRoutes.add(link.route);
  }

  const groupExplicitHeadings = new Map<string, LegendHeading[]>();
  const groupDirHeadings = new Map<string, LegendHeading[]>();

  for (const groupConfig of COMPONENT_GROUPS_CONFIG) {
    groupExplicitHeadings.set(groupConfig.title, []);
    groupDirHeadings.set(groupConfig.title, []);
  }

  for (const groupConfig of COMPONENT_GROUPS_CONFIG) {
    const headings = groupExplicitHeadings.get(groupConfig.title)!;
    for (const member of groupConfig.members) {
      if (!isDirMember(member)) {
        const matched = allParsed.find(
          (parsed) =>
            !claimedRoutes.has(parsed.route) &&
            storyMatchesMember(parsed, member)
        );
        if (matched) {
          claimedRoutes.add(matched.route);
          headings.push(parsedToHeading(matched, currentPath));
        }
      }
    }
  }

  for (const groupConfig of COMPONENT_GROUPS_CONFIG) {
    const headings = groupDirHeadings.get(groupConfig.title)!;
    for (const member of groupConfig.members) {
      if (isDirMember(member)) {
        const dirMatches = allParsed
          .filter(
            (parsed) =>
              !claimedRoutes.has(parsed.route) &&
              storyMatchesMember(parsed, member)
          )
          .sort((alpha, bravo) => alpha.label.localeCompare(bravo.label));
        for (const matched of dirMatches) {
          claimedRoutes.add(matched.route);
          headings.push(parsedToHeading(matched, currentPath));
        }
      }
    }
  }

  const groupSections: LegendSection[] = [];
  for (const groupConfig of COMPONENT_GROUPS_CONFIG) {
    const headers = [
      ...(groupExplicitHeadings.get(groupConfig.title) ?? []),
      ...(groupDirHeadings.get(groupConfig.title) ?? []),
    ].sort((alpha, bravo) => alpha.label.localeCompare(bravo.label));
    if (headers.length > 0) {
      const overviewConfig = SECTION_OVERVIEW_CONFIG.find(
        (config) => config.title === groupConfig.title
      );
      const overviewRoute = overviewConfig
        ? `/section/${overviewConfig.slug}`
        : null;
      groupSections.push(
        makeContainerSection(
          groupConfig.title,
          headers,
          currentPath,
          overviewRoute
        )
      );
    }
  }

  const ungroupedHeadings = allParsed
    .filter((parsed) => !claimedRoutes.has(parsed.route))
    .sort((alpha, bravo) => alpha.label.localeCompare(bravo.label))
    .map((parsed) => parsedToHeading(parsed, currentPath));

  const ungroupedSection: LegendSection[] =
    ungroupedHeadings.length > 0
      ? [
          makeContainerSection(
            "Ungrouped",
            ungroupedHeadings,
            currentPath,
            null
          ),
        ]
      : [];

  return [
    makeLinkSection("/get-started", "Get started", currentPath),
    makeContainerSection(
      "Foundations",
      staticLinksToHeadings(FOUNDATIONS_LINKS, currentPath),
      currentPath,
      "/section/foundations"
    ),
    ...groupSections,
    makeContainerSection(
      "Page templates",
      staticLinksToHeadings(PATTERNS_LINKS, currentPath).sort((alpha, bravo) =>
        alpha.label.localeCompare(bravo.label)
      ),
      currentPath,
      "/section/page-templates"
    ),
    makeContainerSection("Prototypes", [], currentPath, "/section/prototypes"),
    makeLinkSection("/DataFetch.other", "Data fetching", currentPath),
    ...ungroupedSection,
  ];
}
