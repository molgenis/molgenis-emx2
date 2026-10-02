import type { FetchGraphqlResponse } from "../../../types/CmsComponents";
import { cmsFetch } from "../cms";
import type { IFile } from "../../../types/types";

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

export async function AddFileList(schema: string, id: string) {
  const query = `mutation insert($fileList:[FileListsInput]) {
    insert(FileLists:$fileList) {
      message
    }
  }`;
  const variables = { fileList: [{ id: `${id}` }] };
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
