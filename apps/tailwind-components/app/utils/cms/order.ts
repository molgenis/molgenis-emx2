import { cmsFetch } from "../cms";
import type {
  ICmsOrder,
  ICmsOrderWithBlockId,
} from "../../../types/CmsComponents";

export async function fullReorder(
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

export async function prepareOrder(
  schema: string,
  order: number,
  block: string
) {
  const query = `query getComponents($filter:ComponentOrdersFilter, $orderby:[ComponentOrdersorderby]) {
    ComponentOrders(filter:$filter,orderby:$orderby) {
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

export async function getBlockOrder(
  schema: string,
  page: string,
  fromOrder: number = 0
): Promise<ICmsOrderWithBlockId[] | undefined> {
  const query = `query getBlocks($filter: BlockOrdersFilter, $orderby:[BlockOrdersorderby]) {
      BlockOrders(filter:$filter,orderby:$orderby) {
        id
        block {
          id
          mg_tableclass
        }
        order
      }
    }`;

  const variables = {
    filter: {
      configurablePage: { equals: [{ name: page }] },
      order: { between: [fromOrder, null] },
    },
    orderby: [{ order: "ASC" }],
  };

  const { data } = await cmsFetch(schema, query, variables);
  if (data?.BlockOrders) {
    const blocksToUpdate = (data.BlockOrders as ICmsOrderWithBlockId[]).map(
      (block: ICmsOrderWithBlockId) => {
        return {
          id: block.id,
          order: block.order,
          block: {
            id: block.block.id,
            mg_tableclass: block.block.mg_tableclass,
          },
        };
      }
    );
    return blocksToUpdate;
  }
}

export async function prepareBlockOrder(
  schema: string,
  order: number,
  page: string
) {
  const blocksToUpdate = await getBlockOrder(schema, page, order);
  const updatedBlocks = blocksToUpdate?.map((block: ICmsOrder) => {
    block.order = block.order + 1;
    return block;
  });

  if (updatedBlocks?.length) {
    const updateQuery = `mutation update($value:[BlockOrdersInput]) {
        update(BlockOrders:$value) {
          message
        }
      }`;

    const updateVars = { value: updatedBlocks };
    await cmsFetch(schema, updateQuery, updateVars);
  }
}

export async function AddOrder(
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

export async function AddBlockOrder(
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
