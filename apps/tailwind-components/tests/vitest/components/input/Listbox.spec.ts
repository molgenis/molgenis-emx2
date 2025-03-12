import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { InputListbox } from "#components";

const wrapper = mount(InputListbox, {
  props: {
    id: "tw-input-listbox-test",
    options: [
      {value: "group_a", label: "Group A"},
      {value: "group_b", label: "Group B"},
      {value: "group_c", label: "Group C"},
    ]
  }
});

describe("listbox", () => {
  
  const button = wrapper.find('button[role="combobox"]');
  const listbox = wrapper.find('ul[role="listbox"]');
  const options = wrapper.findAllComponents('ul[role="listbox"] li[role="option"]');
  
  it("toggle has required aria attributes", async () => {
    const ariaControls = button.attributes("aria-controls");
    const ariaRequired = button.attributes("aria-required");
    const ariaExpanded = button.attributes("aria-expanded");
    const ariaHasPopup = button.attributes("aria-haspopup");
    
    expect(ariaControls).toBeDefined();
    expect(ariaRequired).toEqual("false");
    expect(ariaExpanded).toEqual("false");
    expect(ariaHasPopup).toEqual("listbox");
  });
  
  it("toggle and listbox are linked by ID", async () => {
    const ariaControlsId = button.attributes("aria-controls");
    const listboxId = listbox.attributes("id");
    expect(ariaControlsId).toEqual(listboxId);
  });
  
  it("placeholder is always the first element in the list", async () => {
    const firstOptionText = wrapper.find("li[role='option']:first-child > span");
    expect(firstOptionText.html()).toEqual("<span>Select an option</span>");
  });
  
  it.each([options])("listbox options have aria-selected defined", (elem) => {
    const ariaSelected = elem.attributes("aria-selected");
    if (elem.find("span").html() === "<span>Select an option</span>") {
      expect(ariaSelected).toEqual("true");
    } else {
      expect(ariaSelected).toEqual("false");
    }
  });
})