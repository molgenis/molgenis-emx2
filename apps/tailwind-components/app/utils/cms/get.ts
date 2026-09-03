import type {
  IBlocks,
  IComponents,
  IBlockOrders,
  IComponentOrders,
} from "../../../types/cms";

import { cmsFetch } from "../cms";

export async function getComponents(
  schema: string,
  page?: string
): Promise<IComponents[]> {
  const query = `query getComponents($filter: ComponentsFilter){
    Components(filter: $filter) {
      ...ComponentsAllFields3
    }
  }`;
  const variables = { filter: {} };
  if (page) {
    variables.filter = {
      inBlock: {
        inContainer: {
          name: {
            equals: page,
          },
        },
      },
    };
  }
  const { data } = await cmsFetch(schema, query, variables);
  return data?.Components as IComponents[];
}

export async function getComponentOrders(
  schema: string,
  page?: string
): Promise<IComponentOrders[]> {
  const query = `query getComponentOrders($filter: ComponentOrdersFilter){
    ComponentOrders(filter: $filter) {
      ...ComponentOrdersAllFields3
    }
  }`;
  const variables = { filter: {} };
  if (page) {
    variables.filter = {
      block: {
        inContainer: {
          name: {
            equals: page,
          },
        },
      },
    };
  }
  const { data } = await cmsFetch(schema, query, variables);
  return data?.Components as IComponents[];
}

export async function getBlocks(
  schema: string,
  page?: string
): Promise<IBlocks[]> {
  const query = `query getPageBlocks($filter: BlocksFilter) {
        Blocks(filter: $filter) {
            ...BlocksAllFields3
        }
    }`;
  const variables = { filter: {} };
  if (page) {
    variables.filter = {
      inContainer: {
        name: {
          equals: page,
        },
      },
    };
  }
  const { data } = await cmsFetch(schema, query, variables);
  return data?.Blocks as IBlocks[];
}

export async function getBlockOrders(
  schema: string,
  page?: string
): Promise<IBlocks[]> {
  const query = `query getBlockOrders($filter: BlockOrdersFilter) {
        BlockOrders(filter: $filter) {
            ...BlockOrdersAllFields3
        }
    }`;
  const variables = { filter: {} };
  if (page) {
    variables.filter = {
      configurablePage: {
        name: {
          equals: page,
        },
      },
    };
  }
  const { data } = await cmsFetch(schema, query, variables);
  return data?.Blocks as IBlocks[];
}
