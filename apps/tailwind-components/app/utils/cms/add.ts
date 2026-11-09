import { cmsFetch } from "../cms";
import {
  prepareOrder,
  AddOrder,
  prepareBlockOrder,
  AddBlockOrder,
} from "./order";

import type { FetchGraphqlResponse } from "../../../types/CmsComponents";
import type { IFile } from "../../../types/types";

export async function addComponent(
  schema: string,
  id: string,
  parentBlock: string,
  order: number,
  componentType: string
) {
  await prepareOrder(schema, order, parentBlock);

  const result = await createComponentType(componentType, schema, id);
  if (result) {
    await AddOrder(schema, id, order, parentBlock);
  } else {
    console.warn("Component Type", componentType, "does not exist in the CMS");
  }
}

export async function createComponentType(
  componentType: string,
  schema: string,
  id: string
) {
  let isComponentType: boolean = true;
  switch (componentType) {
    case "Buttons":
      await AddButton(schema, id);
      break;
    case "ColumnCharts":
      await AddColumnChart(schema, id);
      break;
    case "File":
      await AddFile(schema, id);
      break;
    case "FileList":
      await AddFileList(schema, id);
      break;
    case "Heading":
      await AddHeading(schema, id);
      break;
    case "Image":
      await AddImage(schema, id);
      break;
    case "NavigationCards":
      await AddNavigationCard(schema, id);
      break;
    case "OrderedLists":
      break;
    case "Paragraph":
      await AddParagraph(schema, id);
      break;
    case "UnorderedLists":
      await AddUnorderedList(schema, id);
      break;
    default:
      isComponentType = false;
  }

  return isComponentType;
}

export async function addBlock(
  schema: string,
  id: string,
  page: string,
  order: number,
  componentType: string
) {
  await prepareBlockOrder(schema, order, page);
  if (componentType === "Header") {
    await AddHeader(schema, id);
  }
  if (componentType === "Section") {
    await AddSection(schema, id);
  }
  if (componentType === "Section - 2 Columns") {
    await AddSection(schema, id, 2);
  }
  if (componentType === "Section - 3 Columns") {
    await AddSection(schema, id, 3);
  }
  await AddBlockOrder(schema, id, order, page);
}

export async function AddButton(schema: string, id: string) {
  const query = `mutation insert($button:[ButtonsInput]) {
    insert(Buttons:$button) {
      status
      message
    }
  }`;
  const variables = { button: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

export async function AddColumnChart(schema: string, id: string) {
  const query = `mutation insert($element: [StatisticalChartsInput]) {
    insert(StatisticalCharts: $element) {
      status
      message
    }
  }`;

  const variables = {
    element: [
      {
        id: id,
        chartType: { name: "Column chart" },
        chartTitle: "My column chart",
      },
    ],
  };

  await cmsFetch(schema, query, variables);
}

export async function AddFile(schema: string, id: string) {
  const query = `mutation insert($file:[FilesInput]) {
    insert(Files:$file) {
      status
      message
    }
  }`;
  const variables = { file: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

export async function AddFileList(schema: string, id: string) {
  const query = `mutation insert($fileList:[FileListsInput]) {
    insert(FileLists:$fileList) {
      message
    }
  }`;
  const variables = { fileList: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

export async function AddHeader(schema: string, id: string) {
  const query = `mutation insert($header:[HeadersInput]) {
    insert(Headers:$header) {
      message
    }
  }`;

  const variables = {
    header: [
      {
        id: `${id}`,
        title: "Title",
        subtitle: "A subtitle here",
      },
    ],
  };

  await cmsFetch(schema, query, variables);
}

export async function AddHeading(schema: string, id: string) {
  const query = `mutation insert($heading:[HeadingsInput]) {
    insert(Headings:$heading) {
      status
      message
    }
  }`;
  const variables = { heading: [{ id: `${id}`, text: "Section Heading" }] };
  await cmsFetch(schema, query, variables);
}

export async function AddImage(schema: string, id: string) {
  const query = `mutation insert($image:[ImagesInput]) {
    insert(Images:$image) {
      status
      message
    }
  }`;
  const variables = { image: [{ id: `${id}` }] };
  await cmsFetch(schema, query, variables);
}

export async function AddLink(
  schema: string,
  id: string,
  linkToExternalFile: string,
  alternateFileName: string,
  fileTag: string
) {
  const query = `mutation insert($file:[FilesInput]) {
    insert(Files:$file) {
      status
      message
    }
  }`;
  const variables = {
    file: [
      {
        id: `${id}`,
        fileIsAnExternalLink: true,
        linkToExternalFile,
        alternateFileName,
        fileTag,
      },
    ],
  };
  await cmsFetch(schema, query, variables);
}

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
        orderedItems: ["Item 1", "Item 2", "Item 3"],
      },
    ],
  };
  await cmsFetch(schema, query, variables);
}

export async function AddParagraph(schema: string, id: string) {
  const query = `mutation insert($paragraph:[ParagraphsInput]) {
    insert(Paragraphs:$paragraph) {
      status
      message
    }
  }`;
  const variables = { paragraph: [{ id: `${id}`, text: "My new paragraph" }] };
  await cmsFetch(schema, query, variables);
}

async function AddSection(schema: string, id: string, columns: number = 1) {
  const query = `mutation insert($section:[SectionsInput]) {
    insert(Sections:$section) {
      status
      message
    }
  }`;
  const variables = { section: [{ id: `${id}`, columns }] };
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
        unorderedItems: ["Item 1", "Item 2", "Item 3"],
      },
    ],
  };
  await cmsFetch(schema, query, variables);
}

export async function UploadFile(
  schema: string,
  id: string,
  alternateFileName?: string,
  fileTag?: string,
  file?: IFile
) {
  const query = `mutation insert($file:[FilesInput]) {
    insert(Files:$file) {
      status
      message
    }
  }`;
  const formData = new FormData();
  formData.append("query", query);
  formData.append(
    "variables",
    JSON.stringify({
      file: [
        {
          id: `${id}`,
          file: "file",
          alternateFileName,
          fileTag,
          fileIsAnExternalLink: false,
        },
      ],
    })
  );
  formData.append("file", file as Blob);

  const url: string = `/${schema}/graphql`;
  const response = (await $fetch(url, {
    method: "POST",
    body: formData,
  })) as unknown as FetchGraphqlResponse;

  if (response?.errors?.[0]?.message) {
    console.error(response.errors[0].message);
  }
  return response;
}
