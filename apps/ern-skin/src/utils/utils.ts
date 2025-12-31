// generate a sequence between two numbers by a specific increment
export function seqAlong(start: Number, stop: Number, by: Number): Array {
  return Array.from(
    { length: (stop - start) / by + 1 },
    (_, i) => start + i * by
  );
}
