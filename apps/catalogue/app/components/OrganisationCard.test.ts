import { mount } from "@vue/test-utils";
import { describe, expect, test } from "vitest";
import ContactCard from "./ContactCard.vue";
import OrganisationCard from "./OrganisationCard.vue";

const resource = { id: "r1", name: "R", type: [{ name: "Cohort" }] };

const names = [
  {
    case: "organisationName",
    org: {
      organisationName: "Own name",
      organisation: { name: "ROR name" },
      organisationNotInROR: "Not in ROR",
    },
    shown: "Own name",
  },
  {
    case: "the ROR name",
    org: {
      organisation: { name: "ROR name" },
      organisationNotInROR: "Not in ROR",
    },
    shown: "ROR name",
  },
  {
    case: "organisationNotInROR",
    org: { organisationNotInROR: "Not in ROR" },
    shown: "Not in ROR",
  },
];

describe.each(names)("organisation name is $case", ({ org, shown }) => {
  test("OrganisationCard shows it", () => {
    const wrapper = mount(OrganisationCard, {
      props: {
        organisation: {
          id: "o1",
          resource,
          ...org,
        },
      },
    });
    expect(wrapper.text()).toContain(shown);
    expect(wrapper.text().replace(shown, "")).not.toMatch(
      /Own name|ROR name|Not in ROR/
    );
  });

  test("ContactCard shows it", () => {
    const wrapper = mount(ContactCard, {
      props: {
        contact: {
          resource,
          firstName: "A",
          lastName: "Boss",
          displayName: "A Boss",
          organisation: {
            id: "o1",
            resource,
            ...org,
          },
        },
      },
    });
    expect(wrapper.text()).toContain(shown);
    expect(wrapper.text().replace(shown, "")).not.toMatch(
      /Own name|ROR name|Not in ROR/
    );
  });
});
