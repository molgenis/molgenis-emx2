import type {ProcessStatus} from "../../../metadata-utils/src/generic";
import type {AsyncDataRequestStatus} from "#app";

export function isSuccess(status: ProcessStatus | AsyncDataRequestStatus | undefined) {
  return status === "DONE" || status === "INVALID" || status === "success";
}