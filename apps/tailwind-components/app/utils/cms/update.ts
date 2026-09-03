import type { IComponents, IBlocks } from "../../../types/cms";

import { cmsFetch } from "../cms";

export async function updateComponents(
  schema: string,
  components: IComponents[]
) {
  const query = `mutation updateComponents($components: [ComponentsInput]) {
    update(Components: $components) {
      status
      message
    }
  }
  `;
  const variables = { components: components };
  await cmsFetch(schema, query, variables);
}

export async function updateBlocks(schema: string, blocks: IBlocks[]) {
  const query = `mutation updateBlockContainers($blocks:[BlocksInput]) {
    update(Blocks: $blocks) {
      status
      message
    }
  }`;

  const variables = { blocks: blocks };
  await cmsFetch(schema, query, variables);
}
