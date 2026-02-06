import { expect, test, describe } from "vitest";
import { parsePageText } from "../../../app/utils/Pages";

describe("parsePageText:", () => {
  test("extra leading and trailing quotes are removed", () => {
    expect(parsePageText('"This is a quoted string"')).toEqual(
      "This is a quoted string"
    );
    expect(parsePageText('""This is a quoted string""')).toEqual(
      "This is a quoted string"
    );
  });

  test("internal quotes are ignored", () => {
    expect(parsePageText('""This is a "quoted" string""')).toEqual(
      'This is a "quoted" string'
    );
    expect(parsePageText('""This is a \'quoted\' string""')).toEqual(
      "This is a 'quoted' string"
    );
  });

  test("empty vaues are ignored", () => {
    expect(parsePageText()).toBeFalsy();
  });

  test("regular text is not altered", () => {
    expect(parsePageText("this is a normal string")).toEqual(
      "this is a normal string"
    );
  });
});
