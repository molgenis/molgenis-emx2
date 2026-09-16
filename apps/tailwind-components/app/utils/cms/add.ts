import { cmsFetch } from "../cms";

export async function AddNavigationCard(schema: string, id: string) {
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
      },
    ],
  };

  await cmsFetch(schema, query, variables);
}

export async function AddOrderedList(schema: string, id: string) {
  const query = `mutation insert($element: [OrderedListsInput]) {
    insert(OrderedLists: $element) {
      status
      message
    }
  }`;
  const variables = {
    element: [
      {
        id: id,
        items: ["Item 1", "Item 2", "Item 3"],
      },
    ],
  };
  await cmsFetch(schema, query, variables);
}

export async function AddUnorderedList(schema: string, id: string) {
  const query = `mutation insert($element: [UnorderedListsInput]) {
    insert(UnorderedLists: $element) {
      status
      message
    }
  }`;
  const variables = {
    element: [
      {
        id: id,
        items: ["Item 1", "Item 2", "Item 3"],
      },
    ],
  };
  await cmsFetch(schema, query, variables);
}
