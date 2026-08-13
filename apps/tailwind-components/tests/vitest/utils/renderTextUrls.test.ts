import { expect, test, describe } from "vitest";
import { renderTextUrls } from "../../../app/utils/cms";

describe("render paragraph url (cms):", () => {
  test("paragraphs without URLs aren't affected", () => {
    const input = "This is my paragraph.";
    const output = renderTextUrls(input);
    expect(output).toEqual(input);
  });

  test("paragraph with one URL is rendered", () => {
    const input =
      "This paragraph has a url to [molgenis.org](https://molgenis.org)";
    const output = renderTextUrls(input);
    const expected =
      'This paragraph has a url to <a href="https://molgenis.org" class="underline decoration-solid">molgenis.org</a>';
    expect(output).toEqual(expected);
  });

  test("paragraph with multiple URLs is rendered", () => {
    const input =
      "This paragraph has a url to [molgenis.org](https://molgenis.org) and the [molgenis-emx2 repository](https://github.com/molgenis/molgenis-emx2)";
    const output = renderTextUrls(input);
    const expected =
      'This paragraph has a url to <a href="https://molgenis.org" class="underline decoration-solid">molgenis.org</a> and the <a href="https://github.com/molgenis/molgenis-emx2" class="underline decoration-solid">molgenis-emx2 repository</a>';
    expect(output).toEqual(expected);
  });

  test("paragraph with one email is rendered", () => {
    const input =
      "This paragraph has an email to [user@email.com](mailto:user@email.com)";
    const expected =
      'This paragraph has an email to <a href="mailto:user@email.com" class="underline decoration-solid">user@email.com</a>';
    const output = renderTextUrls(input);
    expect(output).toEqual(expected);
  });
});
