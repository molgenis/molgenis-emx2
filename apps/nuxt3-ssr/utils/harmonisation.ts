import type {
  HarmonisationStatus,
  IVariableWithMappings,
  IVariableBase,
} from "~/interfaces/types";

type IRepeatingVariableWithMapping = IVariableWithMappings;
type INonRepeatingVariableWithMapping = IVariableBase & IVariableWithMappings;

/**
 * Returns a matrix of harmonisation status for each variable and collection
 * In case of a repeated variable, the status for toplevel variable is based on the combined status of all its repeats
 * @param variables
 * @param collections
 */
export const calcAggregatedHarmonisationStatus = (
  variables: IVariableWithMappings[],
  collections: { id: string }[]
) => {
  return variables.map((v) => {
    return collections.map((c) => {
      if (!hasAnyMapping(v)) {
        // no mapping
        return "unmapped";
      } else if (v.repeats) {
        // handle repeats
        return calcStatusForAggregatedRepeatingVariable(v, c);
      } else {
        // handle non repeating
        return calcStatusForSingleVariable(v, c);
      }
    });
  });
};

export const calcIndividualVariableHarmonisationStatus = (
  variable: IVariableWithMappings,
  collections: { id: string }[]
) => {
  return collections.map((c) => {
    if (!hasAnyMapping(variable)) {
      // no mapping
      return "unmapped";
    } else if (variable.repeats) {
      // handle repeats
      return [variable, ...variable.repeats].map((v) =>
        calcStatusForSingleVariable(v, c)
      );
    } else {
      // handle non repeating
      return calcStatusForSingleVariable(variable, c);
    }
  });
};

const hasAnyMapping = (variable: IVariableWithMappings) => {
  return (
    Array.isArray(variable.mappings) ||
    (variable.repeats &&
      variable.repeats.filter((r) => Array.isArray(r.mappings)).length)
  );
};

const calcStatusForSingleVariable = (
  variable: INonRepeatingVariableWithMapping,
  collection: { id: string }
): HarmonisationStatus => {
  const collectionMapping = variable.mappings?.find((mapping) => {
    return mapping.sourceDataset.collection.id === collection.id;
  });

  switch (collectionMapping?.match.name) {
    case undefined:
      return "unmapped";
    case "na":
      return "unmapped";
    case "partial":
      return "partial";
    case "complete":
      return "complete";
    default:
      return "unmapped";
  }
};

const calcStatusForAggregatedRepeatingVariable = (
  variable: IRepeatingVariableWithMapping,
  collection: { id: string }
): HarmonisationStatus => {
  const statusList = !variable.repeats
    ? []
    : variable.repeats.map((repeatedVariable) => {
        const resourceMapping = repeatedVariable.mappings?.find((mapping) => {
          return (
            mapping.targetVariable &&
            mapping.targetVariable.name === repeatedVariable.name &&
            mapping.sourceDataset.resource.id === collection.id
          );
        });

        return resourceMapping ? resourceMapping.match.name : "na";
      });

  const baseVariable = variable.mappings?.find((mapping) => {
    return (
      mapping.targetVariable &&
      mapping.targetVariable.name === variable.name &&
      mapping.sourceDataset.resource.id === collection.id
    );
  });

  if (baseVariable) {
    statusList.push(baseVariable.match.name);
  }
  // If all repeats have a mapping and there are no 'NAs', variable is 'complete'
  if (!statusList.includes("na")) {
    return "complete";
    // If some repeats have a mapping but there are 'NAs', variable is 'partial'
  } else if (
    statusList.includes("partial") ||
    statusList.includes("complete")
  ) {
    return "partial";
    // Unmapped when no repeats have a mapping (only NAs)
  } else {
    return "unmapped";
  }
};
