export function formatDate(val: string | null | undefined): string {
	if (!val) return "-";
	try {
		return new Date(val).toLocaleString();
	} catch {
		return val;
	}
}

export interface ProgressSnapshot {
	phase: string | null;
	message: string | null;
	progress: number | null;
}

function normalizeProgressValue(progress: unknown): number | null {
	if (typeof progress !== "number" || Number.isNaN(progress)) return null;
	if (progress < 0 || progress > 1) return null;
	return progress;
}

export function resolveProgressSnapshot(source: {
	phase?: unknown;
	message?: unknown;
	progress?: unknown;
}): ProgressSnapshot {
	const phase = typeof source.phase === "string" ? source.phase : null;
	const message = typeof source.message === "string" ? source.message : null;
	const progress = normalizeProgressValue(source.progress);
	return { phase, message, progress };
}

export function formatProgressPercent(
	progress: number | null | undefined,
): string {
	if (typeof progress !== "number" || Number.isNaN(progress)) return "-";
	return `${Math.round(progress * 100)}%`;
}
