import { LocationQueryValue } from ".nuxt/vue-router";

export const locationQueryValueToObject = (
  value: LocationQueryValue | LocationQueryValue[]
) => {
  // TODO: handle nested object case
  if (value === null) {
    return {};
  } else if (Array.isArray(value)) {
    return value.reduce((acc: Record<string, string>, pair) => {
      if (pair === null) {
        return acc;
      }
      const [key, val] = pair.split("=");
      acc[key] = val;
      return acc;
    }, {});
  } else {
    const [key, val] = value.split("=");
    return { [key]: val };
  }
};
