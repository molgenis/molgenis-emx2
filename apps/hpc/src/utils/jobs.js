export {
  JOB_STATUSES,
  TERMINAL_STATUSES,
  isTerminal,
} from "../generated/protocol.js";

export function formatDate(val) {
  if (!val) return "-";
  try {
    return new Date(val).toLocaleString();
  } catch {
    return val;
  }
}
