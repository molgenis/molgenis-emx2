import type {
  HarmonizationStatus,
  IVariableWithMappings,
  IVariableBase,
} from "~/interfaces/types";

type IRepeatingVariableWithMapping = IVariableWithMappings;
type INonRepeatingVariableWithMapping = IVariableBase & IVariableWithMappings;

/**
 * Returns a matrix of harmonization status for each variable and cohort
 * In case of a repeated variable, the status for toplevel variable is based on the combined status of all its repeats
 * @param variables
 * @param cohorts
 */
export const calcAggregatedHarmonizationStatus = (
  variables: IVariableWithMappings[],
  cohorts: { id: string }[]
) => {
  return variables.map((v) => {
    return cohorts.map((c) => {
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

export const calcIndividualVariableHarmonizationStatus = (
  variable: IVariableWithMappings,
  cohorts: { id: string }[]
) => {
  return cohorts.map((c) => {
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
  cohort: { id: string }
): HarmonizationStatus => {
  const resourceMapping = variable.mappings?.find((mapping) => {
    return mapping.sourceDataset.resource.id === cohort.id;
  });

  switch (resourceMapping?.match.name) {
    case undefined:
      return "unmapped";
    case "na":
      return "unmapped";
    case "partial":
      return "complete";
    case "complete":
      return "complete";
    default:
      return "unmapped";
  }
};

const calcStatusForAggregatedRepeatingVariable = (
  variable: IRepeatingVariableWithMapping,
  cohort: { id: string }
): HarmonizationStatus => {
  const statusList = !variable.repeats
    ? []
    : variable.repeats.map((repeatedVariable) => {
        const resourceMapping = repeatedVariable.mappings?.find((mapping) => {
          return (
            mapping.targetVariable &&
            mapping.targetVariable.name === repeatedVariable.name &&
            mapping.sourceDataset.resource.id === cohort.id
          );
        });

        return resourceMapping ? resourceMapping.match.name : "na";
      });

  const baseVariable = variable.mappings?.find((mapping) => {
    return (
      mapping.targetVariable &&
      mapping.targetVariable.name === variable.name &&
      mapping.sourceDataset.resource.id === cohort.id
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
