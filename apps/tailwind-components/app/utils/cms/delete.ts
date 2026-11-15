import { getChartData } from "./get";
import { cmsFetch, getPage } from "../cms";
import { fullReorder } from "./order";

import type { IConfigurablePages } from "../../../types/cms";
import type {
  IDeleteContainerStatus,
  FetchGraphqlResponse,
} from "../../../types/CmsComponents";

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

export async function deleteComponent(
  schema: string,
  componentId: string,
  componentOrderid: string,
  block: string,
  reorder: boolean = true,
  componentType?: string
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

  if (componentType?.endsWith(".Statistical charts")) {
    await deleteChartData(schema, componentId);
  }

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
        mg_tableclass
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
      component: { id: string; mg_tableclass?: string };
    }[];
    for (const item of itemsToRemove) {
      await deleteComponent(
        schema,
        item.component.id,
        item.id,
        blockId,
        false,
        item.component?.mg_tableclass
      );
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

export async function deleteChartData(schema: string, chartId: string) {
  const data = await getChartData(schema, chartId);

  if (data) {
    const variables = { data: data };
    const query = `mutation DeleteChartData($data: [ChartDataInput]) {
      delete(ChartData: $data) {
        status
        message
      }
    }`;
    return await cmsFetch(schema, query, variables);
  }
}
