import type {
  IConditionsFilter,
  IFilter,
  IPathCondition,
} from "~/interfaces/types";

export function isConditionFilter(
  filter: IConditionsFilter | IFilter
): filter is IConditionsFilter {
  return (filter as IConditionsFilter).conditions !== undefined;
}

export const toPathQueryConditions = (filters: IFilter[]) => {
  const commaSeparatedString = filters
    .filter((f) => !isEmpty(f))
    .map(toPathConditionItem)
    .map((condition) => JSON.stringify(condition))
    .join(",");

  // return as json array if not empty
  return commaSeparatedString ? "[" + commaSeparatedString + "]" : "";
};

export const conditionsFromPathQuery = (
  conditionsString: string
): IPathCondition[] => {
  if (!conditionsString) {
    return [];
  }
  return JSON.parse(conditionsString);
};

const isEmpty = (filter: IFilter) => {
  const type = filter.config.type;
  switch (type) {
    case "SEARCH":
      return filter.search === "";
    case "ONTOLOGY":
    case "REF_ARRAY":
      return (filter as IConditionsFilter).conditions.length === 0;
    default:
      throw new Error(`Unknown filter type: ${type}`);
  }
};

const toPathConditionItem = (filter: IFilter): IPathCondition => {
  const type = filter.config.type;
  switch (type) {
    case "SEARCH":
      return {
        id: filter.id,
        search: filter.search,
      };
    case "ONTOLOGY":
    case "REF_ARRAY":
      return {
        id: filter.id,
        conditions: (filter as IConditionsFilter).conditions,
      };
    default:
      throw new Error(`Unknown filter type: ${type}`);
  }
};
