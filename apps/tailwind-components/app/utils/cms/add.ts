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
