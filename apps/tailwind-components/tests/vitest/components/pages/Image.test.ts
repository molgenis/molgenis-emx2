import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import Image from "../../../../app/components/pages/Image.vue";

const componentWithoutImage = mount(Image, {
  props: {
    id: "vitest-image",
  },
});

const componentWithImage = mount(Image, {
  props: {
    id: "vitest-image",
    image: {
      id: "vitest-image-id",
      size: 1234,
      extension: "jpg",
      url: "path/to/vitest/image/id.jpg",
    },
    alt: "a description about this image",
    width: "325px",
    imageIsCentered: true,
  },
});

describe("CMS: Image component", () => {
  test("displays generic message if image is undefined", () => {
    expect(componentWithoutImage.findAll("img").length).toEqual(0);
    const spanText = componentWithoutImage.find("span").text();
    expect(spanText).toEqual("Click the edit button to upload an image");
  });

  test("displays image correctly with alt description", () => {
    expect(componentWithImage.findAll("span").length).toEqual(0);
    expect(componentWithImage.findAll("img").length).toEqual(1);
    expect(componentWithImage.find("img").attributes("alt")).toEqual(
      "a description about this image"
    );
  });
});
