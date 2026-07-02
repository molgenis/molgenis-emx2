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

type GroupClaimResult = {
  title: string;
  children: LegendHeading[];
  overviewRoute: string | null;
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
  "/pages/Banner.story": "Page banner",
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

const EXAMPLES_LINKS: StaticLink[] = [
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
    slug: "components",
    title: "Components",
    description:
      "The full component library organised by purpose — from basic actions and inputs to complex EMX2-aware widgets.",
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
    slug: "filter",
    title: "Filter",
    description:
      "Search and filter controls for narrowing dataset results — filter search bar and the full filter system panel.",
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
    slug: "navigation",
    title: "Navigation",
    description:
      "Components for moving between pages and sections — navbars, breadcrumbs, pagination, and the sectioned legend.",
  },
  {
    slug: "display",
    title: "Display",
    description:
      "Media and brand elements (images, logos, icon rendering) plus data presentation renderers — display lists, value cells, and text utilities — and page content blocks including headings and paragraphs.",
  },
  {
    slug: "containers",
    title: "Containers",
    description:
      "Structural wrappers — accordions, progressive reveals, and animated transitions.",
  },
  {
    slug: "page-layouts",
    title: "Page layouts",
    description:
      "Chrome components that frame a page — header, page header, footer, and full-page banner.",
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
    slug: "examples-prototypes",
    title: "Examples & prototypes",
    description:
      "Multi-component workflows and interaction patterns demonstrating how components combine in complete scenarios.",
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
  const tree = buildDocsTree(storyModulePaths, "");
  const overviewRoute = `/section/${slug}`;
  const topSection = tree.find((sec) => sec.id === overviewRoute);
  if (topSection) {
    const items = topSection.headers.map((header) => ({
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
  const componentsSection = tree.find((sec) => sec.label === "Components");
  if (componentsSection) {
    const group = componentsSection.headers.find((h) => h.id === overviewRoute);
    if (group?.children) {
      const items = group.children.map((child) => ({
        label: child.label,
        route: child.id,
      }));
      return {
        slug: config.slug,
        title: config.title,
        description: config.description,
        items,
      };
    }
  }
  return null;
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
  const tree = buildDocsTree(storyModulePaths, currentRoute);
  for (const section of tree) {
    if (section.id === currentRoute && section.id.startsWith("/section/")) {
      const slug = section.id.slice("/section/".length);
      const items = section.headers.flatMap((header) =>
        header.children
          ? header.children.map((child) => ({
              label: child.label,
              route: child.id,
            }))
          : [{ label: header.label, route: header.id }]
      );
      return {
        title: section.label,
        slug,
        overviewRoute: section.id,
        items,
      };
    }
    for (const header of section.headers) {
      if (header.children) {
        if (header.id === currentRoute && header.id.startsWith("/section/")) {
          const slug = header.id.slice("/section/".length);
          const items = header.children.map((child) => ({
            label: child.label,
            route: child.id,
          }));
          return {
            title: header.label,
            slug,
            overviewRoute: header.id,
            items,
          };
        }
        if (header.children.some((child) => child.id === currentRoute)) {
          const slug = header.id.startsWith("/section/")
            ? header.id.slice("/section/".length)
            : header.id;
          const items = header.children.map((child) => ({
            label: child.label,
            route: child.id,
          }));
          return {
            title: header.label,
            slug,
            overviewRoute: header.id,
            items,
          };
        }
      } else {
        if (header.id === currentRoute) {
          const slug = section.id.startsWith("/section/")
            ? section.id.slice("/section/".length)
            : section.id;
          const items = section.headers.map((h) => ({
            label: h.label,
            route: h.id,
          }));
          return {
            title: section.label,
            slug,
            overviewRoute: section.id,
            items,
          };
        }
      }
    }
  }
  return null;
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
    members: [{ dir: "input" }],
  },
  {
    title: "Filter",
    members: [{ storyName: "FilterSearch" }, { dir: "filter" }],
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
    title: "Navigation",
    members: [
      { storyName: "Navigation" },
      { storyName: "NavigationNested" },
      { storyName: "BreadCrumbs" },
      { storyName: "Pagination" },
      {
        storyName: "NavigationGroups",
        route: "/pages/NavigationGroups.story",
      },
      { storyName: "Legend" },
    ],
  },
  {
    title: "Display",
    members: [
      { storyName: "DisplayList" },
      { storyName: "Well" },
      { dir: "display" },
      { dir: "value" },
      { dir: "text" },
      { storyName: "Image" },
      { storyName: "Logo" },
      { storyName: "LogoMobile" },
      { storyName: "Icons" },
      { storyName: "Heading", route: "/pages/Heading.story" },
      { storyName: "Paragraph", route: "/pages/Paragraph.story" },
    ],
  },
  {
    title: "Containers",
    members: [
      { storyName: "Accordion" },
      { storyName: "ShowMore" },
      { storyName: "Skeleton" },
      { dir: "transition" },
    ],
  },
  {
    title: "Page layouts",
    members: [
      { storyName: "Header" },
      { storyName: "PageHeader" },
      { storyName: "FooterComponent" },
      { storyName: "Banner", route: "/pages/Banner.story" },
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
      { storyName: "DeleteModal", route: "/form/DeleteModal.story" },
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

function makeLeafHeading(
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

function resolveIsActive(heading: LegendHeading): boolean {
  return typeof heading.isActive === "boolean"
    ? heading.isActive
    : heading.isActive.value;
}

function makeGroupHeading(
  title: string,
  children: LegendHeading[],
  overviewRoute: string | null,
  currentPath: string
): LegendHeading {
  const id = overviewRoute ?? "";
  const isChildActive = children.some(resolveIsActive);
  return {
    id,
    label: title,
    type: "HEADING",
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: currentPath === id || isChildActive,
    children,
  };
}

function makeSection(
  label: string,
  headers: LegendHeading[],
  id: string,
  currentPath: string
): LegendSection {
  const isHeaderActive = headers.some(resolveIsActive);
  return {
    id,
    label,
    type: "SECTION",
    headers,
    errorCount: computed(() => 0),
    isVisible: computed(() => true),
    isActive: currentPath === id || isHeaderActive,
  };
}

function makeFlatSection(
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

function parsedToLeafHeading(
  parsed: ParsedStory,
  currentPath: string
): LegendHeading {
  return makeLeafHeading(parsed.route, parsed.label, currentPath);
}

function staticLinksToLeafHeadings(
  links: StaticLink[],
  currentPath: string
): LegendHeading[] {
  return links.map((link) =>
    makeLeafHeading(link.route, link.label, currentPath)
  );
}

function claimGroupResults(
  allParsed: ParsedStory[],
  claimedRoutes: Set<string>,
  currentPath: string
): GroupClaimResult[] {
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
          headings.push(parsedToLeafHeading(matched, currentPath));
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
          headings.push(parsedToLeafHeading(matched, currentPath));
        }
      }
    }
  }

  const results: GroupClaimResult[] = [];
  for (const groupConfig of COMPONENT_GROUPS_CONFIG) {
    const children = [
      ...(groupExplicitHeadings.get(groupConfig.title) ?? []),
      ...(groupDirHeadings.get(groupConfig.title) ?? []),
    ].sort((alpha, bravo) => alpha.label.localeCompare(bravo.label));
    if (children.length > 0) {
      const overviewConfig = SECTION_OVERVIEW_CONFIG.find(
        (config) => config.title === groupConfig.title
      );
      const overviewRoute = overviewConfig
        ? `/section/${overviewConfig.slug}`
        : null;
      results.push({ title: groupConfig.title, children, overviewRoute });
    }
  }
  return results;
}

function filterTreeWithQuery(
  sections: LegendSection[],
  query: string
): LegendSection[] {
  const regexps = buildWordRegexps(query);
  const filtered: LegendSection[] = [];
  for (const section of sections) {
    if (matchesAllRegexps(section.label, regexps)) {
      filtered.push(section);
      continue;
    }
    const filteredHeaders: LegendHeading[] = [];
    for (const header of section.headers) {
      if (header.children) {
        if (matchesAllRegexps(header.label, regexps)) {
          filteredHeaders.push(header);
        } else {
          const filteredChildren = header.children.filter((child) =>
            matchesAllRegexps(child.label, regexps)
          );
          if (filteredChildren.length > 0) {
            filteredHeaders.push({ ...header, children: filteredChildren });
          }
        }
      } else {
        if (matchesAllRegexps(header.label, regexps)) {
          filteredHeaders.push(header);
        }
      }
    }
    if (filteredHeaders.length > 0) {
      filtered.push({ ...section, headers: filteredHeaders });
    }
  }
  return filtered;
}

export function buildDocsTree(
  storyModulePaths: string[],
  currentPath: string,
  query: string = ""
): LegendSection[] {
  const allParsed = storyModulePaths.map(parseStoryPath);
  const claimedRoutes = new Set<string>();

  for (const link of FOUNDATIONS_LINKS) {
    claimedRoutes.add(link.route);
  }

  const groupResults = claimGroupResults(allParsed, claimedRoutes, currentPath);

  const ungroupedChildren = allParsed
    .filter((parsed) => !claimedRoutes.has(parsed.route))
    .sort((alpha, bravo) => alpha.label.localeCompare(bravo.label))
    .map((parsed) => parsedToLeafHeading(parsed, currentPath));

  const groupHeadings: LegendHeading[] = groupResults.map((result) =>
    makeGroupHeading(
      result.title,
      result.children,
      result.overviewRoute,
      currentPath
    )
  );

  const allGroupHeadings: LegendHeading[] =
    ungroupedChildren.length > 0
      ? [
          ...groupHeadings,
          makeGroupHeading("Ungrouped", ungroupedChildren, null, currentPath),
        ]
      : groupHeadings;

  const foundationsSection = makeSection(
    "Foundations",
    staticLinksToLeafHeadings(FOUNDATIONS_LINKS, currentPath),
    "/section/foundations",
    currentPath
  );

  const componentsSection = makeSection(
    "Components",
    allGroupHeadings,
    "/section/components",
    currentPath
  );

  const examplesSection = makeSection(
    "Examples & prototypes",
    staticLinksToLeafHeadings(EXAMPLES_LINKS, currentPath),
    "/section/examples-prototypes",
    currentPath
  );

  const tree = [foundationsSection, componentsSection, examplesSection];
  return query.trim() === "" ? tree : filterTreeWithQuery(tree, query);
}

function buildDocsLegend(
  storyModulePaths: string[],
  currentPath: string
): LegendSection[] {
  const allParsed = storyModulePaths.map(parseStoryPath);
  const claimedRoutes = new Set<string>();

  for (const link of FOUNDATIONS_LINKS) {
    claimedRoutes.add(link.route);
  }

  const groupResults = claimGroupResults(allParsed, claimedRoutes, currentPath);

  const groupSections: LegendSection[] = groupResults.map((result) =>
    makeFlatSection(
      result.title,
      result.children,
      currentPath,
      result.overviewRoute
    )
  );

  const ungroupedHeadings = allParsed
    .filter((parsed) => !claimedRoutes.has(parsed.route))
    .sort((alpha, bravo) => alpha.label.localeCompare(bravo.label))
    .map((parsed) => makeLeafHeading(parsed.route, parsed.label, currentPath));

  const ungroupedSection: LegendSection[] =
    ungroupedHeadings.length > 0
      ? [makeFlatSection("Ungrouped", ungroupedHeadings, currentPath, null)]
      : [];

  return [
    makeFlatSection(
      "Foundations",
      staticLinksToLeafHeadings(FOUNDATIONS_LINKS, currentPath),
      currentPath,
      "/section/foundations"
    ),
    ...groupSections,
    makeFlatSection(
      "Examples & prototypes",
      staticLinksToLeafHeadings(EXAMPLES_LINKS, currentPath),
      currentPath,
      "/section/examples-prototypes"
    ),
    ...ungroupedSection,
  ];
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
  const filteredSections: LegendSection[] = [];

  for (const section of allSections) {
    const sectionMatches = matchesAllRegexps(section.label, regexps);
    const matchingHeaders = section.headers.filter((header) =>
      matchesAllRegexps(header.label, regexps)
    );
    if (sectionMatches || matchingHeaders.length > 0) {
      filteredSections.push({
        ...section,
        headers: sectionMatches ? section.headers : matchingHeaders,
      });
    }
  }

  return filteredSections;
}
