import { useHead, useState } from "#app";
import type { ProcessData } from "../../../metadata-utils/src/generic";
import { downloadBlob } from "../../../tailwind-components/app/utils/downloadBlob";
import { isSuccess } from "~/util/processUtils";

// Structure: routeSchema -> routeShaclSet -> ProcessData
const shaclSetRuns = useState(
  "shaclSetRuns",
  () => ({} as Record<string, Record<string, ProcessData>>)
);

export function getProcessData(schema: string, shaclSet: string): ProcessData {
  if (!shaclSetRuns.value[schema]) {
    shaclSetRuns.value[schema] = {};
  }
  if (!shaclSetRuns.value[schema][shaclSet]) {
    shaclSetRuns.value[schema][shaclSet] = { status: "UNKNOWN" };
  }
  return shaclSetRuns.value[schema][shaclSet];
}

export async function runShacl(
  processData: ProcessData,
  schema: string,
  shaclSet: string
): Promise<void> {
  if (processData.status === "RUNNING") return;

  processData.output = undefined;
  processData.error = undefined;
  processData.status = "RUNNING";

  const res = await fetch(`/${schema}/api/rdf?validate=${shaclSet}`);
  processData.output = await res.text();

  if (res.status !== 200) {
    processData.status = "ERROR";
    processData.error = `Error (status code: ${res.status})`;
  } else if (validateShaclOutput(processData.output)) {
    processData.status = "DONE";
  } else {
    processData.status = "INVALID";
  }
}

function validateShaclOutput(output: string): boolean {
  return output
    .substring(0, 100)
    .includes("[] a sh:ValidationReport;\n" + "  sh:conforms true.");
}

export function downloadShacl(
  processData: ProcessData | undefined,
  schema: string,
  shaclSet: string
) {
  if (!processData || !isSuccess(processData.status)) return;
  downloadBlob(
    processData?.output,
    "text/turtle",
    `${schema} - shacl - ${shaclSet}.ttl`
  );
}
