import {
  ICohort,
  HarmonizationStatus,
  IVariableWithMappings,
  IVariableBase,
} from "~/interfaces/types";

type IRepeatingVariableWithMapping = IVariableWithMappings;
type INonRepeatingVariableWithMapping = IVariableBase & IVariableWithMappings;

export const calcStatusForSingleVariable = (
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

export const calcStatusForRepeatingVariable = (
  variable: IRepeatingVariableWithMapping,
  cohort: { id: string }
): HarmonizationStatus => {
  const statusList = !variable.repeats
    ? []
    : variable.repeats.map((repeatedVariable) => {
        const resourceMapping = variable.mappings?.find((mapping) => {
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
