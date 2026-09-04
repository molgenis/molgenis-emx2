import { cmsFetch, deleteBlock, getPage } from "../cms";
import type { IConfigurablePages } from "../../../types/cms";
import type { IDeleteContainerStatus } from "../../../types/CmsComponents";

interface IPageBlocksToRemove {
  blockId: string;
  blockOrderId: string;
}

function newDeleteContainerStatus(): IDeleteContainerStatus {
  return { wasDeleted: false, error: undefined };
}

function setErrorMessage(error?: any) {
  return (
    error?.response?._data?.errors?.[0]?.message ||
    error.message ||
    error.statusMessage ||
    error.statusText
  );
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

export async function deleteConfigurablePage(
  schema: string,
  page: string
): Promise<IDeleteContainerStatus> {
  const result = newDeleteContainerStatus();

  try {
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

    result.wasDeleted = true;
  } catch (error: any) {
    result.wasDeleted = false;
    result.error = setErrorMessage(error);
  } finally {
    return result;
  }
}

export async function deleteDeveloperPage(
  schema: string,
  page: string
): Promise<IDeleteContainerStatus> {
  const result = newDeleteContainerStatus();
  try {
    await deleteContainer(schema, page);
    result.wasDeleted = true;
  } catch (error) {
    result.error = setErrorMessage(error);
  } finally {
    return result;
  }
}
