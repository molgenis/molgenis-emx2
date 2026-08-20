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
  ICmsOrder,
} from "../../types/CmsComponents";

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

async function cmsFetch(
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

export async function moveComponentUp(
  schema: string,
  componentId: string,
  block: string
) {
  console.log("Move component up", componentId, block);
}

export async function moveComponentDown(
  schema: string,
  componentId: string,
  block: string
) {
  console.log("Move component down", componentId, block);
}

export async function moveBlockUp(schema: string, block: string, page: string) {
  console.log("Move block up", block, page);
}

export async function moveBlockDown(
  schema: string,
  block: string,
  page: string
) {
  console.log("Move block down", block, page);
}

export async function moveComponentTo(
  schema: string,
  componentId: string,
  oldBlockId: string,
  newBlockId: string,
  order: number
) {
  console.log(
    "Move component to a different block",
    componentId,
    oldBlockId,
    newBlockId,
    order
  );
}
export async function moveBlockTo(
  schema: string,
  blockId: string,
  page: string,
  order: number
) {
  console.log("Move block to a different page", blockId, page, order);
}

export async function deleteComponent(
  schema: string,
  componentId: string,
  componentOrderid: string,
  block: string,
  reorder: boolean = true
) {
  const orderQuery = `mutation delete($orderId:[ComponentOrdersInput]) {
    delete(ComponentOrders:$orderId){
      message
    }
  }`;

  const componentQuery = `mutation delete($componentId:[ComponentsInput]) {
    delete(Components:$componentId) {
      message
    }
  }`;

  const orderVars = { orderId: [{ id: `${componentOrderid}` }] };
  const componentVars = { componentId: [{ id: `${componentId}` }] };

  await cmsFetch(schema, orderQuery, orderVars);
  await cmsFetch(schema, componentQuery, componentVars);

  if (reorder) {
    await fullReorder(schema, block, "Component");
  }
}

async function deleteAllComponentsFromBlock(schema: string, blockId: string) {
  const query = `query getComponents($filter: ComponentOrdersFilter) {
    ComponentOrders(filter:$filter) {
      id
      order
      component {
        id
      }
    }
  }`;

  const variables = {
    filter: { block: { id: { equals: blockId } } },
    orderby: [{ order: "ASC" }],
  };

  const { data } = (await cmsFetch(
    schema,
    query,
    variables
  )) as FetchGraphqlResponse;

  if (data?.ComponentOrders) {
    const itemsToRemove = data.ComponentOrders as {
      id: string;
      component: { id: string };
    }[];
    for (const item of itemsToRemove) {
      await deleteComponent(schema, item.component.id, item.id, blockId, false);
    }
  }
}

export async function deleteBlock(
  schema: string,
  blockId: string,
  blockOrderid: string,
  page: string
) {
  await deleteAllComponentsFromBlock(schema, blockId);

  const orderQuery = `mutation delete($pkey:[BlockOrdersInput]){
    delete(BlockOrders:$pkey) {
      message
    }
  }`;

  const blockQuery = `mutation delete($pkey:[BlocksInput]){
    delete(Blocks:$pkey) {
      message
    }
  }`;

  const orderVar = { pkey: [{ id: `${blockOrderid}` }] };
  const blockVars = { pkey: [{ id: `${blockId}` }] };

  await cmsFetch(schema, orderQuery, orderVar);
  await cmsFetch(schema, blockQuery, blockVars);
  await fullReorder(schema, page, "Block");
}

export async function addComponent(
  schema: string,
  id: string,
  parentBlock: string,
  order: number,
  componentType: string
) {
  await prepareOrder(schema, order, parentBlock);
  if (componentType === "Paragraph") {
    await AddParagraph(schema, id);
  }
  if (componentType === "Heading") {
    await AddHeading(schema, id);
  }
  if (componentType === "Image") {
    await AddImage(schema, id);
  }
  await AddOrder(schema, id, order, parentBlock);
}

export async function addBlock(
  schema: string,
  id: string,
  page: string,
  order: number,
  componentType: string
) {
  await prepareBlockOrder(schema, order, page);
  if (componentType === "Header") {
    await AddHeader(schema, id);
  }
  if (componentType === "Section") {
    await AddSection(schema, id);
  }
  await AddBlockOrder(schema, id, order, page);
}

async function AddSection(schema: string, id: string) {
  const query = `mutation insert($section:[SectionsInput]) {
    insert(Sections:$section) {
      message
    }
  }`;
  const variables = { section: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

async function AddHeader(schema: string, id: string) {
  const query = `mutation insert($header:[HeadersInput]) {
    insert(Headers:$header) {
      message
    }
  }`;

  const variables = {
    header: [
      {
        id: `${id}`,
        title: "Title",
        subtitle: "A subtitle here",
        backgroundImage: { id: "penguins" },
      },
    ],
  };

  await cmsFetch(schema, query, variables);
}

async function AddImage(schema: string, id: string) {
  const query = `mutation insert($image:[ImagesInput]) {
    insert(Images:$image) {
      status
      message
    }
  }`;
  const variables = { image: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

async function AddHeading(schema: string, id: string) {
  const query = `mutation insert($heading:[HeadingsInput]) {
    insert(Headings:$heading) {
      status
      message
    }
  }`;
  const variables = { heading: [{ id: `${id}`, text: "Section Heading" }] };
  await cmsFetch(schema, query, variables);
}

async function AddParagraph(schema: string, id: string) {
  const query = `mutation insert($paragraph:[ParagraphsInput]){
    insert(Paragraphs:$paragraph){
      status
      message
    }
  }`;
  const variables = { paragraph: [{ id: `${id}`, text: "My new paragraph" }] };
  await cmsFetch(schema, query, variables);
}

async function fullReorder(
  schema: string,
  parent: string,
  type: "Component" | "Block"
) {
  let filter: any = {
    block: { id: { equals: parent } },
  };
  if (type === "Block") {
    filter = {
      configurablePage: { name: { equals: parent } },
    };
  }

  const query = `query get${type}s($filter: ${type}OrdersFilter) {
    ${type}Orders(filter:$filter) {
      id
      order
    }
  }`;

  const variables = { filter: filter, orderby: [{ order: "ASC" }] };

  const { data } = await cmsFetch(schema, query, variables);
  const items = type === "Block" ? data?.BlockOrders : data?.ComponentOrders;

  if (items) {
    let order: number = 0;
    const itemsToUpdate = (items as ICmsOrder[])
      .sort((a: ICmsOrder, b: ICmsOrder) => a.order - b.order)
      .map((item: ICmsOrder) => {
        return { id: item.id, order: order++ };
      });

    if (itemsToUpdate.length) {
      const query = `mutation update($value:[${type}OrdersInput]) {
        update(${type}Orders:$value){
          message
        }
      }`;
      const variables = { value: itemsToUpdate };
      await cmsFetch(schema, query, variables);
    }
  }
}

async function prepareOrder(schema: string, order: number, block: string) {
  const query = `query getComponents($filter:ComponentOrdersFilter) {
    ComponentOrders(filter:$filter) {
      id
      order
    }
  }`;
  const variables = {
    filter: {
      block: { id: { equals: block } },
      order: { between: [order, null] },
    },
    orderby: [{ order: "ASC" }],
  };

  const { data } = await cmsFetch(schema, query, variables);

  if (data?.ComponentOrders) {
    const componentsToUpdate = (data.ComponentOrders as ICmsOrder[]).map(
      (item: ICmsOrder) => {
        return { id: item.id, order: item.order + 1 };
      }
    );

    if (componentsToUpdate.length) {
      const updateQuery = `mutation update($value:[ComponentOrdersInput]) {
        update(ComponentOrders:$value) {
          message
        }
      }`;
      const updateVars = { value: componentsToUpdate };
      await cmsFetch(schema, updateQuery, updateVars);
    }
  }
}

async function prepareBlockOrder(schema: string, order: number, page: string) {
  const query = `query getBlocks($filter: BlockOrdersFilter) {
    BlockOrders(filter:$filter) {
      id
      order
    }
  }`;

  const variables = {
    filter: {
      configurablePage: { equals: [{ name: page }] },
      order: { between: [order, null] },
    },
    orderby: [{ order: "ASC" }],
  };

  const { data } = await cmsFetch(schema, query, variables);

  if (data?.BlockOrders) {
    const blocksToUpdate = (data.BlockOrders as ICmsOrder[]).map(
      (block: ICmsOrder) => {
        return { id: block.id, order: block.order + 1 };
      }
    );

    if (blocksToUpdate.length) {
      const updateQuery = `mutation update($value:[BlockOrdersInput]) {
        update(BlockOrders:$value) {
          message
        }
      }`;

      const updateVars = { value: blocksToUpdate };
      await cmsFetch(schema, updateQuery, updateVars);
    }
  }
}

async function AddOrder(
  schema: string,
  id: string,
  order: number,
  parentBlock: string
) {
  const query = `mutation insert($value:[ComponentOrdersInput]) {
    insert(ComponentOrders:$value) {
      message
    }
  }`;

  const variables = {
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
  };
  await cmsFetch(schema, query, variables);
}

async function AddBlockOrder(
  schema: string,
  id: string,
  order: number,
  page: string
) {
  const query = `mutation insert($value:[BlockOrdersInput]) {
    insert(BlockOrders:$value) {
      message
    }
  }`;

  const variables = {
    value: [
      {
        id: `${id}-order`,
        configurablePage: {
          name: page,
        },
        block: {
          id: `${id}`,
        },
        order: order,
      },
    ],
  };
  await cmsFetch(schema, query, variables);
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

export function renderTextUrls(string: string): string {
  let paragraph = string;
  const urlPattern = /\[(.*?)\]\((.*?)\)/g;
  paragraph = paragraph.replaceAll(
    urlPattern,
    '<a href="$2" class="underline decoration-solid">$1</a>'
  );
  return paragraph;
}
