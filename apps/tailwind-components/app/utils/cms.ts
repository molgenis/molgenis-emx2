import type {
  IConfigurablePages,
  IDeveloperPages,
  IDependenciesCSS,
  IDependenciesJS,
} from "../../types/cms";

import { getContainersQuery } from "../gql/cmsPages";

import type {
  IContainerMetadata,
  ICmsJsFetchPriority,
  FetchGraphqlResponse,
} from "../../types/CmsComponents";

export const actionsToolbarShowDelay = 200;
export const actionsToolbarHideDelay = 300;

export function randomId(): string {
  return crypto.randomUUID();
}

export function newDeveloperPage(initialHtml?: string): IDeveloperPages {
  return {
    mg_tableclass: "",
    name: "",
    description: "",
    html: initialHtml || "",
    css: "",
    javascript: "",
    dependencies: [],
    enableBaseStyles: true,
    enableButtonStyles: true,
    enableFullScreen: false,
  };
}

export async function getPage(
  schema: string,
  page: string
): Promise<IContainerMetadata> {
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: getContainersQuery,
      variables: { filter: { name: { equals: page } } },
    },
  });

  const currentPage = data.Containers[0] as
    | IConfigurablePages
    | IDeveloperPages;
  return { page: currentPage, metadata: data._schema.tables };
}

export function setCmsPageType(value?: string): string | undefined {
  if (!value || typeof value === "undefined") {
    return undefined;
  } else if (value.endsWith(".Configurable pages")) {
    return "Landing page";
  } else if (value.endsWith(".Developer pages")) {
    return "Dev page";
  } else {
    return undefined;
  }
}

export function setCmsEditorUrl(
  schema: string,
  value: string,
  page: string
): string {
  if (value.endsWith(".Developer pages")) {
    return `/${schema}/pages/${page}/editor`;
  } else {
    return `/${schema}/pages/${page}/configure`;
  }
}

export function setCmsViewUrl(schema: string, page: string): string {
  return `/${schema}/pages/${page}/`;
}

export async function cmsFetch(
  schema: string,
  query: string,
  variables?: any
): Promise<FetchGraphqlResponse> {
  const url: string = `/${schema}/graphql`;
  const response = (await $fetch(url, {
    method: "POST",
    body: { query: query, variables: variables },
  })) as unknown as FetchGraphqlResponse;

  if (response?.errors?.[0]?.message) {
    console.error(response.errors[0].message);
  }

  return response;
}

export function generateHtmlPreview(
  content: IDeveloperPages,
  ref: HTMLDivElement
) {
  const parser = new DOMParser();

  if (content && typeof content === "object" && Object.keys(content).length) {
    ref.replaceChildren();

    const documentHead = document.getElementsByTagName(
      "head"
    )[0] as HTMLHeadElement;

    if (content.dependencies) {
      renderHtmlPreviewDependencies(documentHead, content.dependencies);
    }

    if (content.html) {
      const doc = parser.parseFromString(content.html, "text/html");
      Array.from(doc.body.children).forEach((element) => {
        ref.appendChild(element);
      });
    }

    if (content.css) {
      const styleElement = document.createElement("style");
      styleElement.textContent = content.css;
      documentHead.appendChild(styleElement);
    }

    if (content.javascript) {
      const scriptElement = document.createElement("script");
      scriptElement.setAttribute("type", "text/javascript");
      scriptElement.text = `setTimeout(() => {
        /** timeout is required for correctly loading external dependencies */
        ${content.javascript}
      }, 200)`;
      ref.appendChild(scriptElement);
    }
  } else {
    generateLegacyHtmlPreview(parser, ref, content);
  }
}

function renderHtmlPreviewDependencies(
  documentHead: HTMLHeadElement,
  dependencies: IDependenciesCSS[] | IDependenciesJS[]
) {
  dependencies.forEach((dependency: IDependenciesCSS | IDependenciesJS) => {
    if (dependency.mg_tableclass?.endsWith("CSS") && dependency.url) {
      const elem = document.createElement("link");
      elem.href = dependency.url;
      elem.rel = "stylesheet";
      documentHead.appendChild(elem);
    }

    if (dependency.mg_tableclass?.endsWith("JS") && dependency.url) {
      const jsDependency = dependency as IDependenciesJS;

      const elem = document.createElement("script") as HTMLScriptElement;
      elem.src = jsDependency.url as string;

      if (elem.src && jsDependency.async) {
        elem.async = jsDependency.async as boolean;
      }

      if (elem.src && !jsDependency.async && jsDependency.defer) {
        elem.defer = jsDependency.defer as boolean;
      }

      if (elem.src && jsDependency.fetchPriority) {
        elem.fetchPriority = jsDependency.fetchPriority
          .name as ICmsJsFetchPriority;
      }
    }
  });
}

function generateLegacyHtmlPreview(
  parser: DOMParser,
  ref: HTMLDivElement,
  content: IDeveloperPages
) {
  const htmlString: string = content as unknown as string;
  const doc = parser.parseFromString(htmlString, "text/html");
  /** Loop over the just parsed html items and add them */
  Array.from(doc.body.children).forEach((el) => {
    if (el.tagName !== "SCRIPT") {
      ref.appendChild(el);
    } else {
      /** Script tags need a special treatment, else they will not execute. **/
      const scriptEl = document.createElement("script");
      if ((el as HTMLScriptElement).src) {
        /** If we have an external script. */
        scriptEl.src = (el as HTMLScriptElement).src;
      } else {
        /** Regular inline script */
        scriptEl.textContent = el.textContent;
      }
      ref.appendChild(scriptEl);
    }
  });
}

export function parsePageText(value?: string): string {
  const val = value || "";
  return val.replace(/(^"{1,})|("{1,}$)/g, "");
}

export function pageCopyDate(): string {
  const date = new Date().toISOString();
  return date.replace("T", " ").split(".")[0] as string;
}

export async function updateCMSfield(
  schema: string,
  typeName: "Paragraphs" | "Headings",
  id: string,
  text: string
) {
  const updateQuery = `mutation update($value:[${typeName}Input]) {
      update(${typeName}:$value) {
        message
      }
    }`;

  const updateVars = { value: [{ id, text }] };
  await cmsFetch(schema, updateQuery, updateVars);
}
