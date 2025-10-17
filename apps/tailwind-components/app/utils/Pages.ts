export interface TreeNode {
  name: string;
  children?: TreeNode[];
  parent?: {
    name: string;
  };
}

export interface OntologyNode extends TreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
  order?: number;
}

export interface Dependency {
  name: string;
  url?: string;
  fetchPriority?: OntologyNode;
  mg_tableclass: string;
}

export interface DependencyCSS extends Dependency {
  type?: OntologyNode;
}

export interface DependencyJS extends Dependency {
  type?: OntologyNode;
  async?: boolean;
  defer?: boolean;
}

export interface DeveloperPage {
  name: string;
  description?: string;
  html?: string;
  css?: string;
  javascript?: string;
  dependencies?: DependencyCSS[] | DependencyJS[];
  enableBaseStyles?: boolean;
  enableButtonStyles?: boolean;
  enableFullScreen?: boolean;
}

export interface Pages {
  name: string;
  description?: string;
}

export function newDeveloperPage(): DeveloperPage {
  return {
    name: "",
    description: "",
    html: "",
    css: "",
    javascript: "",
    dependencies: [],
    enableBaseStyles: true,
    enableButtonStyles: true,
    enableFullScreen: false,
  };
}

export function newPageDate(): string {
  const date = new Date().toISOString();
  return date.replace("T", " ").split(".")[0] as string;
}

export async function getPage(
  schema: string,
  page: string
): Promise<DeveloperPage> {
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `query getDeveloperPage($filter:DeveloperPageFilter) {
        DeveloperPage(filter:$filter) {
          name
          description
          html
          css
          javascript
          dependencies {
            mg_tableclass
            name
            url
            defer
            async
            fetchPriority {
              name
            }
            async
            defer
          }
          enableBaseStyles
          enableButtonStyles
          enableFullScreen
        }
      }`,
      variables: { filter: { name: { equals: page } } },
    },
  });
  return data.DeveloperPage[0];
}

export function generateHtmlPreview(
  content: DeveloperPage,
  ref: HTMLDivElement
) {
  if (content && typeof content === "object" && Object.keys(content).length) {
    ref.replaceChildren();

    const parser = new DOMParser();
    const documentHead = document.getElementsByTagName(
      "head"
    )[0] as HTMLHeadElement;

    content.dependencies?.forEach(
      (dependency: DependencyCSS | DependencyJS) => {
        if (dependency.mg_tableclass.endsWith("CSS") && dependency.url) {
          const elem = document.createElement("link");
          elem.href = dependency.url;
          elem.rel = "stylesheet";
          documentHead.appendChild(elem);
        }

        if (dependency.mg_tableclass.endsWith("JS") && dependency.url) {
          const jsDependency = dependency as DependencyJS;

          const elem = document.createElement("script") as HTMLScriptElement;
          elem.src = jsDependency.url as string;

          if (elem.src && jsDependency.async) {
            elem.async = jsDependency.async as boolean;
          }

          if (elem.src && !jsDependency.async && jsDependency.defer) {
            elem.defer = jsDependency.defer as boolean;
          }

          if (elem.src && jsDependency.fetchPriority) {
            elem.fetchPriority = jsDependency.fetchPriority.name as
              | "high"
              | "low"
              | "auto";
          }
        }
      }
    );

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
    const parser = new DOMParser();
    const htmlString: string = content as unknown as string;
    const doc = parser.parseFromString(htmlString, "text/html");
    /** Loop over the just parsed html items, and add them */
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
}
