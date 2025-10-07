import { mount } from "@vue/test-utils";
import { expect, it } from "vitest";
import Pagination from "../../../components/Pagination.vue";

const wrapper = mount(Pagination, {
  props: {
    currentPage: 3,
    totalPages: 34,
    preventDefault: true,
    inverted: false,
    jumpToEdge: false,
  },
});

it("should render the pagination component (without jump to edge)", async () => {
  expect(wrapper.html()).matchSnapshot();
  expect(wrapper.html()).not.toContain("doublearrowleft");
  expect(wrapper.html()).not.toContain("doublearrowright");
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
  expect(withEdge.html()).toContain("doublearrowleft");
  expect(withEdge.html()).toContain("doublearrowright");
});

it("should emit update event on button click", async () => {
  const first = wrapper.find("a").find("span");
  await first.trigger("click");
  expect(wrapper.emitted()).toHaveProperty("update");
  expect(wrapper.emitted("update")?.[0]).toEqual([2]);
  const last = wrapper.findAll("a").at(1)?.find("span");
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
  const first = firstPage.find("a").find("span");
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
  const last = lastPage.findAll("a").at(1)?.find("span");
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
  const first = firstPage.findAll("a").at(0)?.find("span");
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
  const last = lastPage.findAll("a").at(3)?.find("span");
  await last?.trigger("click");
  expect(lastPage.emitted("update")).toBeUndefined();
});
