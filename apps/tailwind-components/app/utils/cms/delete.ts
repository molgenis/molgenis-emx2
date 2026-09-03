import {
  getBlocks,
  getBlockOrders,
  getComponents,
  getComponentOrders,
} from "./get";

import { updateComponents, updateBlocks } from "./update";

import type {
  IConfigurablePages,
  IDeveloperPages,
  IDependenciesCSS,
  IDependenciesJS,
  IBlocks,
  IComponents,
} from "../../../types/cms";

import { cmsFetch } from "../cms";

async function deleteContainer(schema: string, page: string) {
  const query = `mutation deleteContainer($container:[ContainersInput]) {
    delete(Containers: $container) {
      status
      message
    }
  }`;
  const variables = { container: { name: page } };
  return await cmsFetch(schema, query, variables);
}

async function deletePageComponentRefs(schema: string, page: string) {
  const pageBlocks = await getBlocks(schema, page);
  const pageComponents = await getComponents(schema, page);

  if (pageComponents && pageBlocks) {
    const pageBlockIds = pageBlocks.map((block) => block.id);

    // If a component is used in multiple blocks, remove refs to
    // blocks on the current page and keep everything else
    const componentsWithMultipleRefs = pageComponents
      .filter((component) => {
        return component.inBlock && component.inBlock.length > 1;
      })
      .map((component) => {
        const newComponent = structuredClone(component);
        newComponent.inBlock = newComponent.inBlock.filter(
          (block: IBlocks) => !pageBlockIds.includes(block.id)
        );
        return newComponent;
      });

    if (componentsWithMultipleRefs) {
      await updateComponents(schema, componentsWithMultipleRefs);
    }
  }
}

async function deletePageComponentOrderRefs(schema: string, page: string) {
  const pageComponentOrders = await getComponentOrders(schema, page);
  if (pageComponentOrders) {
    for (const order of pageComponentOrders) {
      // await deletePageComponentOrder(schema, id);
    }
  }
}

async function deletePageBlockRefs(schema: string, page: string) {
  const pageBlocks = await getBlocks(schema, page);
  if (pageBlocks) {
    const blocksWithMultipleRefs = pageBlocks
      .filter((block) => block.inContainer && block.inContainer.length > 1)
      .map((block) => {
        const newBlock = structuredClone(block);
        newBlock.inContainer = newBlock.inContainer.filter((container: any) => {
          return container.name !== page;
        });
        return newBlock;
      });

    if (blocksWithMultipleRefs) {
      await updateBlocks(schema, blocksWithMultipleRefs);
    }
  }
}

function deletePageBlockOrderRefs(schema: string, page: string) {}

export async function deleteConfigurablePage(schema: string, page: string) {
  await deletePageComponentRefs(schema, page);
  await deletePageBlockRefs(schema, page);
  await deleteContainer(schema, page);
}

export async function deleteDeveloperPage(schema: string, page: string) {
  const query = `mutation deleteContainer($container:[ContainersInput]) {
    delete(Containers:$container) {
      status
      message
    }
  }`;
  const variables = { container: { name: page } };
  return await cmsFetch(schema, query, variables);
}
