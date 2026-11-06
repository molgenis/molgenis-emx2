import { cmsFetch } from "../cms";
import {
  getBlockOrder,
  prepareBlockOrder,
  fullReorder,
  prepareOrder,
} from "./order";

async function getBlockAbove(
  schema: string,
  blockOrderId: string,
  page: string
): Promise<{ id: string; type: string } | undefined> {
  const blockOrders = await getBlockOrder(schema, page);

  let lastBlock: { id: string; type: string } | undefined;
  blockOrders?.every((block, index) => {
    if (block.block.id === blockOrderId && index > 0) {
      return false;
    } else {
      lastBlock = { id: block.block.id, type: block.block.mg_tableclass };
      return true;
    }
  });
  return lastBlock;
}

async function getBlockBelow(
  schema: string,
  blockId: string,
  page: string
): Promise<{ id: string; type: string } | undefined> {
  const blockOrders = await getBlockOrder(schema, page);
  let blockBelow: { id: string; type: string } | undefined;
  blockOrders?.every((block, index) => {
    if (block.block.id === blockId) {
      return false;
    } else {
      const selected = blockOrders[index + 2];
      if (selected) {
        blockBelow = {
          id: selected?.block.id,
          type: selected.block.mg_tableclass,
        };
      }
      return true;
    }
  });
  return blockBelow;
}

export async function moveComponentUp(
  schema: string,
  componentOrderId: string,
  order: number,
  blockId: string,
  page: string
) {
  if (order === 0) {
    const blockAbove = await getBlockAbove(schema, blockId, page);
    if (!blockAbove || blockAbove?.type !== "cms.Sections") {
      return;
    }
    await moveComponentTo(
      schema,
      componentOrderId,
      blockId,
      blockAbove.id,
      1000
    );
  } else {
    await moveComponentTo(
      schema,
      componentOrderId,
      blockId,
      blockId,
      order - 1
    );
  }
}
async function GetLastOrderOfBlock(
  schema: string,
  blockId: string
): Promise<number> {
  const query = `query getComponents($filter:ComponentOrdersFilter, $orderby:[ComponentOrdersorderby]) {
    ComponentOrders(filter:$filter,orderby:$orderby) {
      order
    }
  }`;
  const variables = {
    filter: {
      block: { id: { equals: blockId } },
    },
    orderby: [{ order: "DESC" }],
  };
  const { data } = await cmsFetch(schema, query, variables);

  if (data?.ComponentOrders?.[0]) {
    return data.ComponentOrders[0].order || 0;
  }
  return 0;
}

export async function moveComponentDown(
  schema: string,
  componentOrderId: string,
  order: number,
  blockId: string,
  page: string
) {
  if (order === (await GetLastOrderOfBlock(schema, blockId))) {
    const blockBelow = await getBlockBelow(schema, blockId, page);
    if (!blockBelow || blockBelow?.type !== "cms.Sections") {
      return;
    }

    await moveComponentTo(schema, componentOrderId, blockId, blockBelow.id, 0);
  } else {
    await moveComponentTo(
      schema,
      componentOrderId,
      blockId,
      blockId,
      order + 2
    );
  }
}

export async function moveBlockUp(
  schema: string,
  blockOrderId: string,
  order: number,
  page: string
) {
  const newOrder = Math.max(0, order - 1);
  const query = `mutation update($value:[BlockOrdersInput]) {
    update(BlockOrders:$value) {
      message
    }
  }`;

  const vars = {
    value: {
      id: blockOrderId,
      order: newOrder,
    },
  };

  await prepareBlockOrder(schema, newOrder, page);
  await cmsFetch(schema, query, vars);
  await fullReorder(schema, page, "Block");
}

export async function moveBlockDown(
  schema: string,
  blockOrderId: string,
  order: number,
  page: string
) {
  const newOrder = order + 1;
  const query = `mutation update($value:[BlockOrdersInput]) {
    update(BlockOrders:$value) {
      message
    }
  }`;

  const vars = {
    value: {
      id: blockOrderId,
      order: newOrder,
    },
  };

  await prepareBlockOrder(schema, newOrder + 1, page);
  await cmsFetch(schema, query, vars);
  await fullReorder(schema, page, "Block");
}

export async function moveComponentTo(
  schema: string,
  componentOrderId: string,
  oldBlockId: string,
  newBlockId: string,
  order: number
) {
  const query = `mutation update($value:[ComponentOrdersInput]) {
    update(ComponentOrders:$value) {
      message
    }
  }`;

  const vars = {
    value: {
      id: componentOrderId,
      block: {
        id: newBlockId,
      },
      order: order,
    },
  };
  await prepareOrder(schema, order, newBlockId);
  await cmsFetch(schema, query, vars);
  await fullReorder(schema, oldBlockId, "Component");
  if (oldBlockId !== newBlockId) {
    await fullReorder(schema, newBlockId, "Component");
  }
}

export async function moveBlockTo(
  schema: string,
  blockOrderId: string,
  page: string,
  order: number
) {
  const query = `mutation update($value:[BlockOrdersInput]) {
    update(BlockOrders:$value) {
      message
    }
  }`;

  const vars = {
    value: {
      id: blockOrderId,
      order: order,
    },
  };
  await prepareBlockOrder(schema, order, page);
  await cmsFetch(schema, query, vars);
}
