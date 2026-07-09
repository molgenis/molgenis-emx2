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
} from "../../types/CmsComponents";

export function newDeveloperPage(): IDeveloperPages {
  return {
    mg_tableclass: "",
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

export async function addComponent(
  schema: string,
  id: string,
  parentBlock: string,
  order: number,
  componentType: string
) {
  console.log("addComponent", schema, id, parentBlock, order, componentType);
  if (componentType === "Paragraph") {
    await prepareOrder(schema, order, parentBlock);
    await AddParagraph(schema, id, order, parentBlock);
  }
  console.log("addComponent done");
  // TODO: do we need to redo the order numbers?
}

export async function prepareOrder(
  schema: string,
  order: number,
  block: string,
) {
  // get every component in block with order >= order and increment their order by 1
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `query getComponents($filter: ComponentOrdersFilter){ComponentOrders(filter:$filter){id,order}}`,
      variables: {
        filter: {
          block: { id: { equals: block } },
          order: {between: [order, null]},
        },
        orderby: [{order: "ASC"}]
      },
    },
  });
  console.log("prepareOrder", data);

  const componentsToUpdate = data.ComponentOrders as {
    id: string;
    order: number;
  }[];

  // TODO: combine the updates into a single mutation instead of doing one per component
  for (const component of componentsToUpdate) {
    await $fetch(`/${schema}/graphql`, {
      method: "POST",
      body: {
        query: `mutation update($value:[ComponentOrdersInput]){update(ComponentOrders:$value){message}}`,
        variables: {
          value: [
            {
              id: component.id,
              order: component.order + 1,
            },
          ],
        },
      },
    });
  }
  // 
}

export async function AddParagraph(
  schema: string,
  id: string,
  order: number,
  parentBlock: string
): Promise<boolean> {
  // add the paragraph component
  const { data } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `mutation insert($value:[ParagraphsInput]){insert(Paragraphs:$value){message}}`,
      variables: {
        value: [
          {
            paragraphIsCentered: false,
            text: "add your text here",
            id: `${id}`,
          },
        ],
      },
    },
  });
  console.log("AddParagraph", data);
  // add the order and location
  const { data_order } = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `mutation insert($value:[ComponentOrdersInput]){insert(ComponentOrders:$value){message}}`,
      variables: {
        value: [
          {
            id: `${id}-order`,
            block: {
              id: parentBlock,
            },
            component: {
              id: `${id}`,
            },
            order: order,
          },
        ],
      },
    },
  });
  console.log("AddParagraph order", data_order);
  return true;
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
