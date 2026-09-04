import { cmsFetch, deleteBlock, getPage } from "../cms";
import type { IConfigurablePages } from "../../../types/cms";

interface IPageBlocksToRemove {
  blockId: string;
  blockOrderId: string;
}

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

export async function deleteConfigurablePage(schema: string, page: string) {
  // remove blocks and components
  const pageData = await getPage(schema, page);
  const currentPage = pageData.page as IConfigurablePages;

  const pageBlocksToRemove = currentPage.blockOrder?.map((block) => {
    return { blockId: block.block.id, blockOrderId: block.id };
  }) as IPageBlocksToRemove[];

  for (const block of pageBlocksToRemove) {
    await deleteBlock(schema, block.blockId, block.blockOrderId, page);
  }

  // delete page: all refs should be removed
  await deleteContainer(schema, page);
}

export async function deleteDeveloperPage(schema: string, page: string) {
  await deleteContainer(schema, page);
}
