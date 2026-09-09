import { cmsFetch } from "../cms";

export async function AddNavigationGroup(schema: string, id: string) {
  const query = `mutation insert($nav:[NavigationGroupsInput]) {
    insert(NavigationGroups: $nav) {
      status
      message
    }
  }`;
  const variables = { nav: [{ id: id }] };
  await cmsFetch(schema, query, variables);
}

export async function AddNavigationCard(
  schema: string,
  id: string,
  parentId: string
) {
  const query = `mutation insert($element: [NavigationCardsInput]) {
    insert(NavigationCards: $element) {
      status
      message
    }
  }`;
  const variables = {
    element: [
      {
        id: id,
        title: "Title",
        description: "A description about the link",
        url: "https://molgenis.org",
        displayedInNavigationGroup: { id: parentId },
      },
    ],
  };

  await cmsFetch(schema, query, variables);
}
