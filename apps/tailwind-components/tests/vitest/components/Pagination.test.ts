import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import Pagination from "../../../app/components/Pagination.vue";

const wrapper = mount(Pagination, {
  props: {
    currentPage: 3,
    totalPages: 34,
    preventDefault: true,
    inverted: false,
    jumpToEdge: false,
  },
});

it("should render the pagination component with jump to edge", async () => {
  const withEdge = mount(Pagination, {
    props: {
      currentPage: 3,
      totalPages: 34,
      preventDefault: true,
      inverted: false,
      jumpToEdge: true,
    },
  });
  expect(withEdge.html()).matchSnapshot();
  expect(withEdge.html()).not.toStrictEqual(wrapper.html());
});

it("should emit update event on button click", async () => {
  const first = wrapper.find("button").find("span");
  await first.trigger("click");
  expect(wrapper.emitted()).toHaveProperty("update");
  expect(wrapper.emitted("update")?.[0]).toEqual([2]);
  const last = wrapper.findAll("button").at(1)?.find("span");
  await last?.trigger("click");
  expect(wrapper.emitted("update")?.[1]).toEqual([4]);
});

it("should not emit update event when on first page and clicking first", async () => {
  const firstPage = mount(Pagination, {
    props: {
      currentPage: 1,
      totalPages: 34,
      preventDefault: true,
      inverted: false,
      jumpToEdge: false,
    },
  });
  const first = firstPage.find("button").find("span");
  await first.trigger("click");
  expect(firstPage.emitted("update")).toBeUndefined();
});

it("should not emit update event when on last page and clicking last", async () => {
  const lastPage = mount(Pagination, {
    props: {
      currentPage: 34,
      totalPages: 34,
      preventDefault: true,
      inverted: false,
      jumpToEdge: false,
    },
  });
  const last = lastPage.findAll("button").at(1)?.find("span");
  await last?.trigger("click");
  expect(lastPage.emitted("update")).toBeUndefined();
});

it("should not emit update when jumpToEdge is true and clicking first on first page", async () => {
  const firstPage = mount(Pagination, {
    props: {
      currentPage: 1,
      totalPages: 34,
      preventDefault: true,
      inverted: false,
      jumpToEdge: true,
    },
  });
  const first = firstPage.findAll("button").at(0)?.find("span");
  await first?.trigger("click");
  expect(firstPage.emitted("update")).toBeUndefined();
});

it("should not emit update when jumpToEdge is true and clicking last on last page", async () => {
  const lastPage = mount(Pagination, {
    props: {
      currentPage: 34,
      totalPages: 34,
      preventDefault: true,
      inverted: false,
      jumpToEdge: true,
    },
  });
  const last = lastPage.findAll("button").at(3)?.find("span");
  await last?.trigger("click");
  expect(lastPage.emitted("update")).toBeUndefined();
});

it("renders prev and next controls when showPageSelector is false, but not the page-number box", () => {
  const wrapper = mount(Pagination, {
    props: {
      currentPage: 3,
      totalPages: 34,
      showPageSelector: false,
    },
  });
  const anchors = wrapper.findAll("button");
  expect(anchors.length).toBe(2);
  expect(anchors[0].text()).toContain("Go to page 2");
  expect(anchors[1].text()).toContain("Go to page 4");
  expect(wrapper.find("input").exists()).toBe(false);
});

it("disables the prev control at page 1 via Button's own disabled prop, but not in the middle of a range", () => {
  const atStart = mount(Pagination, {
    props: { currentPage: 1, totalPages: 34 },
  });
  const prevAtStart = atStart.findAll("button")[0];
  expect(prevAtStart.attributes("disabled")).toBe("");

  const inRange = mount(Pagination, {
    props: { currentPage: 17, totalPages: 34 },
  });
  const prevInRange = inRange.findAll("button")[0];
  expect(prevInRange.attributes("disabled")).toBeUndefined();
});

it("disables the next control at the last page, but not in the middle of a range", () => {
  const atEnd = mount(Pagination, {
    props: { currentPage: 34, totalPages: 34 },
  });
  const nextAtEnd = atEnd.findAll("button")[1];
  expect(nextAtEnd.attributes("disabled")).toBe("");

  const inRange = mount(Pagination, {
    props: { currentPage: 17, totalPages: 34 },
  });
  const nextInRange = inRange.findAll("button")[1];
  expect(nextInRange.attributes("disabled")).toBeUndefined();
});

it("disables first and last at their respective boundaries when jumpToEdge is on", () => {
  const atStart = mount(Pagination, {
    props: { currentPage: 1, totalPages: 34, jumpToEdge: true },
  });
  const [first, , , last] = atStart.findAll("button");
  expect(first.attributes("disabled")).toBe("");
  expect(last.attributes("disabled")).toBeUndefined();

  const atEnd = mount(Pagination, {
    props: { currentPage: 34, totalPages: 34, jumpToEdge: true },
  });
  const [firstAtEnd, , , lastAtEnd] = atEnd.findAll("button");
  expect(lastAtEnd.attributes("disabled")).toBe("");
  expect(firstAtEnd.attributes("disabled")).toBeUndefined();
});

it("renders the four directional controls as Button, type tertiary, size small", () => {
  // jsdom loads no stylesheet here, so getComputedStyle never resolves a
  // custom property; the class that carries the token is the closest proxy.
  const wrapper = mount(Pagination, {
    props: { currentPage: 17, totalPages: 34, jumpToEdge: true },
  });
  wrapper.findAll("button").forEach((button) => {
    expect(button.classes()).toEqual(
      expect.arrayContaining([
        "bg-button-tertiary",
        "text-button-tertiary",
        "border-button-tertiary",
        "h-button-small",
      ])
    );
  });
});

it("floors and caps the prev/next sr-only page numbers at the real range", () => {
  const atStart = mount(Pagination, {
    props: { currentPage: 1, totalPages: 34 },
  });
  const prevAtStart = atStart.findAll("button")[0].find("span");
  expect(prevAtStart.text()).toBe("Go to page 1");

  const atEnd = mount(Pagination, {
    props: { currentPage: 34, totalPages: 34 },
  });
  const nextAtEnd = atEnd.findAll("button")[1].find("span");
  expect(nextAtEnd.text()).toBe("Go to page 34");
});
