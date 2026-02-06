import { expect, test, describe } from "vitest";
import {
  IConfigurablePages,
  IBlocks,
  IParagraphs,
  IHeadings,
  IComponents,
} from "../../../types/cms";
import { sortConfigurablePage } from "../../../app/utils/Pages";

describe("Sort configurable pages", () => {
  test("Blocks are returned as is if no order is defined", () => {
    const page: IConfigurablePages = {
      name: "test-page",
      blocks: [
        {
          id: "section-welcome",
        },
        {
          id: "section-about-us",
        },
      ],
    };
    const sortedPage = sortConfigurablePage(page);
    const sortedBlocks = sortedPage.blocks?.map((item: IBlocks) => item.id);
    expect(sortedBlocks).toEqual(["section-welcome", "section-about-us"]);
  });

  test("All blocks are sorted according to definition and blocks without ordering are sorted last", () => {
    const page: IConfigurablePages = {
      name: "test-page",
      blockOrder: [
        {
          id: "DVJpz37HffyN",
          block: {
            id: "section-welcome",
          },
          order: 1,
        },
        {
          id: "DUZDtVsKPRoY",
          block: {
            id: "section-about-us",
          },
          order: 0,
        },
      ],
      blocks: [
        {
          id: "section-welcome",
        },
        {
          id: "section-about-us",
        },
        {
          id: "section-contact-us",
        },
      ],
    };
    const sortedPage = sortConfigurablePage(page);
    const sortedBlocks = sortedPage.blocks?.map((item: IBlocks) => item.id);
    expect(sortedBlocks).toEqual([
      "section-about-us",
      "section-welcome",
      "section-contact-us",
    ]);
  });

  test("Components in a block are returned as is if no order is defined", () => {
    const page: IConfigurablePages = {
      name: "test-page",
      blocks: [
        {
          id: "section-welcome",
          components: [
            {
              id: "section-welcome-heading",
              text: "Welcome to my page",
              level: 2,
            } as IHeadings,
            {
              id: "section-welcome-text",
              text: "Proident dolor sint nulla officia deserunt incididunt ad esse consectetur qui magna cillum.",
            } as IParagraphs,
          ],
        },
      ],
    };

    const sortedPage = sortConfigurablePage(page);
    const sortedComponents = (
      sortedPage.blocks as IBlocks[]
    )[0].components?.map((item: IComponents) => item.id);
    expect(sortedComponents).toEqual([
      "section-welcome-heading",
      "section-welcome-text",
    ]);
  });

  test("All components are sorted according to preferred order and all other components are last", () => {
    const page: IConfigurablePages = {
      name: "test-page",
      blocks: [
        {
          id: "section-welcome",
          components: [
            {
              id: "section-welcome-heading",
              text: "Welcome to my page",
              level: 2,
            } as IHeadings,
            {
              id: "section-welcome-text",
              text: "Proident dolor sint nulla officia deserunt incididunt ad esse consectetur qui magna cillum.",
            } as IParagraphs,
            {
              id: "section-welcome-text-2",
              text: "Pariatur et excepteur esse sunt ea ipsum anim ullamco et.",
            } as IParagraphs,
          ],
          componentOrder: [
            {
              id: "3mMEKlKL6qGi",
              component: {
                id: "section-welcome-heading",
              },
              order: 1,
            },
            {
              id: "TtuY11J0PCuz",
              component: {
                id: "section-welcome-text",
              },
              order: 0,
            },
          ],
        },
      ],
    };

    const sortedPage = sortConfigurablePage(page);
    const componentOrder = (sortedPage.blocks as IBlocks[])[0].components?.map(
      (item: IComponents) => item.id
    );
    expect(componentOrder).toEqual([
      "section-welcome-text",
      "section-welcome-heading",
      "section-welcome-text-2",
    ]);
  });
});
