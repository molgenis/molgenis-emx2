import { expect, it, describe } from "vitest";
import { readableFileSize } from "../../../app/utils/readableFileSize";

describe("readableFileSize", () => {
  it("should return a better readable file size", () => {
    expect(readableFileSize(1023)).toBe("1023 Bytes");
    expect(readableFileSize(1024)).toBe("1 KB");
    expect(readableFileSize(1024 * 1024)).toBe("1 MB");
    expect(readableFileSize(1024 * 1024 * 1024)).toBe("1.00 GB");
    expect(readableFileSize(1024 * 1024 * 1024 * 1024)).toBe("1.00 TB");
  });
});
