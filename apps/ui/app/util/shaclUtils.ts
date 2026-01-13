import { useHead, useState } from "#app";
import type { ProcessData } from "../../../metadata-utils/src/generic";
import { downloadBlob } from "../../../tailwind-components/app/utils/downloadBlob";
import { isSuccess } from "./processUtils";

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
  schema: string,
  shaclSet: string
): Promise<void> {
  const processData = getProcessData(schema, shaclSet);

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

export function downloadShacl(schema: string, shaclSet: string) {
  const processData = getProcessData(schema, shaclSet);
  if (!isSuccess(processData.status)) return;
  downloadBlob(
    processData?.output,
    "text/turtle",
    `${schema} - shacl - ${shaclSet}.ttl`
  );
}
