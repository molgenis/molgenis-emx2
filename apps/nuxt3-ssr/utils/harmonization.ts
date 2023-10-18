import { ICohort, IMapping, HarmonizationStatus } from "~/interfaces/types";

interface IRepeatingVariableWithMapping {
  name: string;
  label: string;
  description: string;
  mappings?: IMapping[];
  repeats?: {
    name: string;
    mappings: IMapping[];
  }[];
}

interface INonRepeatingVariableWithMapping {
  name: string;
  label: string;
  description: string;
  mappings?: IMapping[];
}

export const calcStatusForSingleVariable = (
  variable: INonRepeatingVariableWithMapping,
  cohort: ICohort
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
  cohort: ICohort
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
