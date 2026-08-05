import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";

import PageBanner from "../../../../app/components/pages/Banner.vue";

const defaultBanner = mount(PageBanner, {
  props: {
    id: "vitest-page-banner",
    title: "My Page Banner",
    subtitle: "this is an example",
  },
});

const demoImage = "/path/to/some/image.jpg";

const bannerWithImage = mount(PageBanner, {
  props: {
    id: "vitest-page-banner",
    title: "My Page Banner",
    subtitle: "this is an example",
    backgroundImage: { id: demoImage },
    image: { id: demoImage, url: demoImage },
  },
});

describe("Custom Pages: banner", () => {
  test("by default, banners have a title and a subtitle", async () => {
    expect(defaultBanner.find("h1").exists()).toBeTruthy();
    expect(defaultBanner.find("p").exists()).toBeTruthy();
  });

  test("when an image is defined, the image filter and colors are applied", async () => {
    console.log(bannerWithImage.html());

    expect(bannerWithImage.find("h1").exists()).toBeTruthy();
    expect(bannerWithImage.find("p").exists()).toBeTruthy();
    expect(bannerWithImage.classes()).toContain("text-gray-100");

    const filter = bannerWithImage.find("div:nth-child(2)");
    expect(filter.exists()).toBeTruthy();
    expect(filter.attributes("class")).toContain(
      "absolute top-0 left-0 w-full h-full bg-black bg-opacity-60"
    );
  });
});
