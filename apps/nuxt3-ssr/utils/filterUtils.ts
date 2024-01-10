import type { IConditionsFilter, IFilter } from "../interfaces/types";

export const toPathQuery = (filters: IFilter[]) => { 
    return filters
        .filter((f) => !isEmpty(f))
        .map(toPathConditionItem)
        .map((condition) => JSON.stringify(condition))
        .join(",") || undefined; // undefined is used to remove the query param from the URL
}

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
}

const toPathConditionItem = (filter: IFilter) => {
    const type = filter.config.type;
    switch (type) {
        case "SEARCH":
            return {
                id: filter.id,
                search: filter.search
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
}