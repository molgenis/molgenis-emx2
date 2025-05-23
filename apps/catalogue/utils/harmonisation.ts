import type {
  HarmonisationStatus,
  IVariableWithMappings,
  IVariableBase,
} from "~/interfaces/types";

type IRepeatingVariableWithMapping = IVariableWithMappings;
type INonRepeatingVariableWithMapping = IVariableBase & IVariableWithMappings;

/**
 * Returns a matrix of harmonisation status for each variable and source
 * In case of a repeated variable, the status for toplevel variable is based on the combined status of all its repeats
 */
export const calcAggregatedHarmonisationStatus = (
  variables: IVariableWithMappings[],
  resources: { id: string }[]
) => {
  return variables.map((v) => {
    return resources.map((c) => {
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
  resources: { id: string }[]
) => {
  return resources.map((resource) => {
    if (!hasAnyMapping(variable)) {
      // no mapping
      return "unmapped";
    } else if (variable.repeats) {
      // handle repeats
      return [variable, ...variable.repeats].map((v) =>
        calcStatusForSingleVariable(
          variable as INonRepeatingVariableWithMapping,
          resource
        )
      );
    } else {
      // handle non repeating
      return calcStatusForSingleVariable(variable, resource);
    }
  });
};

const hasAnyMapping = (variable: IVariableWithMappings) => {
  return Array.isArray(variable.mappings);
};

const calcStatusForSingleVariable = (
  variable: INonRepeatingVariableWithMapping,
  resource: { id: string }
): HarmonisationStatus => {
  const resourceMapping = variable.mappings?.find((mapping) => {
    return mapping.sourceDataset.resource.id === resource.id;
  });

  switch (resourceMapping?.match.name) {
    case undefined:
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
  resource: { id: string }
): HarmonisationStatus => {
  const statusList = !variable.repeats
    ? []
    : // @ts-ignore
      variable.repeats.map((repeatedVariable) => {
        // @ts-ignore
        const resourceMapping = repeatedVariable.mappings?.find((mapping) => {
          return (
            mapping.targetVariable &&
            mapping.targetVariable.name === repeatedVariable.name &&
            mapping.sourceDataset.resource.id === resource.id
          );
        });

        return resourceMapping ? resourceMapping.match.name : "na";
      });

  const baseVariable = variable.mappings?.find((mapping) => {
    return (
      mapping.targetVariable &&
      mapping.targetVariable.name === variable.name &&
      mapping.sourceDataset.resource.id === resource.id
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
