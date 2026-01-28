import type {
  IDeveloperPages,
  IDependencies,
  IDependenciesCSS,
  IDependenciesJS,
  IBlocks,
  IBlockOrders,
  IConfigurablePages,
  IComponents,
  IComponentOrders,
} from "../../types/cms";

export function newDeveloperPage(): IDeveloperPages {
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

export function parsePageText(value: string): string {
  return value.replace(/(^")|("$)/g, "");
}

export function newPageDate(): string {
  const date = new Date().toISOString();
  return date.replace("T", " ").split(".")[0] as string;
}

export async function getPage(
  schema: string,
  page: string
): Promise<IDeveloperPages | IConfigurablePages> {
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `query getContainers($filter:ContainersFilter) {
        Containers(filter:$filter) {
            
            # Containers
            name
            description
            mg_tableclass
            
            # Developer pages
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
            
            # Configurable pages
            blocks {
                id
                enableFullScreenWidth
                mg_tableclass
                
                # page headings
                title
                subtitle
                backgroundImage {
                    image {
                        id
                        url
                    }
                }
                titleIsCentered
                
                # page sections
                components {
                    id
                    inBlock {
                        id
                    }
                    mg_tableclass
                    
                    # TextElements
                    text
                    
                    # Headings
                    level
                    headingIsCentered
                    
                    # Paragraphs
                    paragraphIsCentered
                    
                    # images
                    displayName
                    image {
                        id
                        url
                    }
                    alt
                    width
                    height
                }
                    
                # component order
                componentOrder {
                    block {
                        id
                    }
                    component {
                        id
                    }
                    order
                }
            }
                
            # block order
            blockOrder {
                block {
                    id
                }
                order
            }
        }
      }`,
      variables: { filter: { name: { equals: page } } },
    },
  });

  const currentPage = data.Containers[0];

  if (currentPage.blockOrder) {
    const blockOrdering = currentPage.blockOrder.reduce(
      (acc: Record<string, number>, blockOrder: IBlockOrders) => {
        return {
          ...acc,
          [blockOrder.block.id]: blockOrder.order,
        };
      },
      {}
    );

    currentPage.blocks = currentPage.blocks.sort((a: IBlocks, b: IBlocks) => {
      return blockOrdering[a.id] - blockOrdering[b.id];
    });
  }

  if (currentPage.blocks.find((block: IBlocks) => block.componentOrder)) {
    currentPage.blocks = currentPage.blocks.map((block: IBlocks) => {
      if (block.componentOrder && block.components) {
        const componentOrder: Record<string, any> = block.componentOrder.reduce(
          (acc: Record<string, number>, componentOrder: IComponentOrders) => {
            return {
              ...acc,
              [componentOrder.component.id]: componentOrder.order,
            };
          },
          {}
        );

        block.components = block.components.sort(
          (a: IComponents, b: IComponents) => {
            return componentOrder[a.id] - componentOrder[b.id];
          }
        );
      }
      return block;
    });
  }

  return currentPage;
}

export function generateHtmlPreview(
  content: IDeveloperPages,
  ref: HTMLDivElement
) {
  if (content && typeof content === "object" && Object.keys(content).length) {
    ref.replaceChildren();

    const parser = new DOMParser();
    const documentHead = document.getElementsByTagName(
      "head"
    )[0] as HTMLHeadElement;

    (content.dependencies as IDependencies[])?.forEach(
      (dependency: IDependenciesCSS | IDependenciesJS) => {
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
