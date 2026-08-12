"""Tests for pr_triage — the wiring, the handlers, and the workflow YAML itself.

A test belongs here when it exercises main(), an event handler, or a function that
mixes a decision with I/O. Pure verdicts go to test_pr_triage_decide.py and HTTP
calls to test_pr_triage_github.py. The banned-term fixtures live here on purpose:
this is the one file FILES_HOLDING_BANNED_TERMS_AS_LITERAL_FIXTURES excludes.

New test file here? The workflow discovers test*.py, so name it accordingly.
"""

import contextlib
import datetime
import fnmatch
import json
import os
import shlex
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage
import pr_triage_decide
import pr_triage_github
import validate_pr_triage_teams

DEFAULT_ITERATIONS = [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}]
DEFAULT_STATUS_OPTIONS = [{"id": "STATUS_OPT_WORKING", "name": "🛠️ Working"}, {"id": "STATUS_OPT_REVIEW", "name": "🔍 Review"}]
DEFAULT_TEAM_OPTIONS = [{"id": "TEAM_OPT_DEV", "name": "Dev"}]

# The board's real Status options, all eight, as returned live -- every option
# carries an emoji prefix from GitHub except Icebox.
LIVE_STATUS_OPTIONS = [
    {"id": "285bb9d7", "name": "\U0001f4cb Epic"},
    {"id": "ff9f2e2b", "name": "⛔️ Blocked"},
    {"id": "f75ad846", "name": "\U0001f4da Backlog"},
    {"id": "47fc9ee4", "name": "\U0001f6e0️ Working"},
    {"id": "879449e7", "name": "\U0001f50d Review"},
    {"id": "98236657", "name": "✅ Done"},
    {"id": "58e93f55", "name": "\U0001f4e5 Inbox"},
    {"id": "7164e058", "name": "Icebox"},
]


def query_of(call):
    return (call["body"] or {}).get("query", "")


def make_fake_http_request(
    calls,
    *,
    assign_result=None,
    assign_error=None,
    draft_result=None,
    draft_error=None,
    board_item=None,
    status_options=None,
    team_options=None,
    iterations=None,
    add_item_id="ITEM_1",
    extra=None,
):
    """One fake GitHub for every pr_triage test: records every call, and answers
    each request shape pr_triage_github.http_request can make. `board_item` is what
    find_board_item_for_pr's query resolves to (None -> no existing item).
    `extra(url, token, method, body, query, variables)` is consulted first and,
    if it returns something other than None, that is the response -- the escape
    hatch for a test that needs one response to differ from these defaults."""
    if status_options is None:
        status_options = DEFAULT_STATUS_OPTIONS
    if team_options is None:
        team_options = DEFAULT_TEAM_OPTIONS
    if iterations is None:
        iterations = DEFAULT_ITERATIONS

    def fake_http_request(url, token, method="GET", body=None):
        calls.append({"url": url, "token": token, "body": body})
        query = (body or {}).get("query", "")
        variables = (body or {}).get("variables", {})

        if extra is not None:
            result = extra(url, token, method, body, query, variables)
            if result is not None:
                return result

        if url.endswith("/assignees"):
            if assign_error is not None:
                raise assign_error
            if assign_result is not None:
                return assign_result
            return {"assignees": [{"login": login} for login in (body or {}).get("assignees", [])]}

        if "convertPullRequestToDraft" in query:
            if draft_error is not None:
                raise draft_error
            if draft_result is not None:
                return draft_result
            return {
                "data": {
                    "convertPullRequestToDraft": {
                        "pullRequest": {"id": variables.get("pullRequestId"), "isDraft": True}
                    }
                }
            }

        if "projectItems" in query:
            nodes = []
            if board_item is not None:
                nodes = [
                    {
                        "id": board_item["id"],
                        "project": {"id": pr_triage_github.BOARD_PROJECT_ID},
                        "status": {"name": board_item["status"]} if board_item.get("status") else None,
                        "team": {"name": board_item["team"]} if board_item.get("team") else None,
                        "sprint": {"title": board_item["sprint"]} if board_item.get("sprint") else None,
                    }
                ]
            return {"data": {"node": {"projectItems": {"nodes": nodes}}}}

        if "addProjectV2ItemById" in query:
            return {"data": {"addProjectV2ItemById": {"item": {"id": add_item_id}}}}

        if "updateProjectV2ItemFieldValue" in query:
            return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": variables.get("itemId")}}}}

        if "options { id name }" in query:
            if variables["fieldId"] == pr_triage_github.STATUS_FIELD_ID:
                return {"data": {"node": {"options": status_options}}}
            if variables["fieldId"] == pr_triage_github.TEAM_FIELD_ID:
                return {"data": {"node": {"options": team_options}}}

        if "configuration" in query:
            return {"data": {"node": {"configuration": {"iterations": iterations}}}}

        raise AssertionError(f"unexpected call: {url} {query}")

    return fake_http_request


def _item_node(item):
    return {
        "id": item["id"],
        "project": {"id": pr_triage_github.BOARD_PROJECT_ID},
        "status": {"name": item["status"]} if item.get("status") else None,
        "team": {"name": item["team"]} if item.get("team") else None,
        "sprint": {"title": item["sprint"]} if item.get("sprint") else None,
    }


def make_fake_http_request_for_redirect(
    calls,
    *,
    pr_item=None,
    closing_issues=(),
    user_linked_issues=(),
    issue_items=None,
    add_item_ids=None,
    status_options=None,
    team_options=None,
    iterations=None,
    assign_result=None,
):
    """One fake GitHub for aim-6 (closing-PR redirection) tests: distinguishes
    a PullRequest node from an Issue node by the `contentId` variable, so a
    single fake covers both the PR's own lookup (which also carries
    closingIssuesReferences / userLinkedClosingIssues) and each linked
    issue's lookup (which never does, exactly like a real Issue fragment)."""
    issue_items = {} if issue_items is None else issue_items
    add_item_ids = {} if add_item_ids is None else add_item_ids
    if status_options is None:
        status_options = LIVE_STATUS_OPTIONS
    if team_options is None:
        team_options = [{"id": "TEAM_DEV_OPT", "name": "Dev"}, {"id": "TEAM_DELIVERY_OPT", "name": "Delivery"}]
    if iterations is None:
        iterations = DEFAULT_ITERATIONS

    def fake(url, token, method="GET", body=None):
        calls.append({"url": url, "token": token, "body": body})
        query = (body or {}).get("query", "")
        variables = (body or {}).get("variables", {})

        if url.endswith("/assignees"):
            if assign_result is not None:
                return assign_result
            return {"assignees": [{"login": login} for login in (body or {}).get("assignees", [])]}

        if "convertPullRequestToDraft" in query:
            return {
                "data": {
                    "convertPullRequestToDraft": {
                        "pullRequest": {"id": variables.get("pullRequestId"), "isDraft": True}
                    }
                }
            }

        if "closingIssuesReferences" in query:
            content_id = variables["contentId"]
            if content_id in issue_items:
                item = issue_items[content_id]
                nodes = [] if item is None else [_item_node(item)]
                return {"data": {"node": {"projectItems": {"nodes": nodes}}}}
            nodes = [] if pr_item is None else [_item_node(pr_item)]
            return {
                "data": {
                    "node": {
                        "projectItems": {"nodes": nodes},
                        "closingIssuesReferences": {"nodes": list(closing_issues)},
                        "userLinkedClosingIssues": {"nodes": list(user_linked_issues)},
                    }
                }
            }

        if "deleteProjectV2Item" in query:
            return {"data": {"deleteProjectV2Item": {"deletedItemId": variables["itemId"]}}}

        if "addProjectV2ItemById" in query:
            content_id = variables["contentId"]
            new_id = add_item_ids.get(content_id, f"NEW_{content_id}")
            return {"data": {"addProjectV2ItemById": {"item": {"id": new_id}}}}

        if "updateProjectV2ItemFieldValue" in query:
            return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": variables.get("itemId")}}}}

        if "options { id name }" in query:
            if variables["fieldId"] == pr_triage_github.STATUS_FIELD_ID:
                return {"data": {"node": {"options": status_options}}}
            return {"data": {"node": {"options": team_options}}}

        if "configuration" in query:
            return {"data": {"node": {"configuration": {"iterations": iterations}}}}

        raise AssertionError(f"unexpected call: {url} {query}")

    return fake


def run_main(event, *, http_request=None, current_date=None, env_extra=None):
    """The tempdir + event.json + summary file + env + mock.patch scaffold every
    main() test needs, collapsed to one call. Returns (summary_text, exit_code):
    exit_code is None when main() returned normally, or sys.exit's code when it
    didn't -- callers that only care about the happy path can ignore it."""
    with tempfile.TemporaryDirectory() as tmp_dir:
        event_path = os.path.join(tmp_dir, "event.json")
        with open(event_path, "w", encoding="utf-8") as handle:
            json.dump(event, handle)
        summary_path = os.path.join(tmp_dir, "summary.md")
        open(summary_path, "w", encoding="utf-8").close()

        env = {
            "GITHUB_EVENT_PATH": event_path,
            "GITHUB_TOKEN": "github-token",
            "PROJECT_BOARD_TOKEN": "board-token",
            "GITHUB_STEP_SUMMARY": summary_path,
        }
        if env_extra:
            env.update(env_extra)

        exit_code = None
        try:
            with contextlib.ExitStack() as stack:
                stack.enter_context(mock.patch.dict(os.environ, env, clear=False))
                if http_request is not None:
                    stack.enter_context(mock.patch.object(pr_triage_github, "http_request", side_effect=http_request))
                if current_date is not None:
                    stack.enter_context(mock.patch.object(pr_triage, "current_date", return_value=current_date))
                pr_triage.main()
        except SystemExit as exit_call:
            exit_code = exit_call.code

        with open(summary_path, encoding="utf-8") as handle:
            return handle.read(), exit_code


class ValidateNoBotOrMachineLoginsTest(unittest.TestCase):
    def test_flags_a_bot_login(self):
        mapping = {"dependabot[bot]": "Dev"}

        errors = validate_pr_triage_teams.find_bot_or_machine_logins(mapping)

        self.assertEqual(errors, ["dependabot[bot]"])

    def test_flags_the_known_machine_accounts(self):
        mapping = {"JV-CI-CD": "Dev", "molgenis-jenkins": "Dev"}

        errors = validate_pr_triage_teams.find_bot_or_machine_logins(mapping)

        self.assertCountEqual(errors, ["JV-CI-CD", "molgenis-jenkins"])

    def test_passes_a_clean_mapping(self):
        mapping = {"mswertz": "Dev"}

        errors = validate_pr_triage_teams.find_bot_or_machine_logins(mapping)

        self.assertEqual(errors, [])


class ValidateTeamValuesTest(unittest.TestCase):
    def test_flags_a_team_value_that_is_not_a_live_board_option(self):
        mapping = {"mswertz": "Nonexistent"}

        errors = validate_pr_triage_teams.find_unknown_team_values(mapping, valid_team_names={"Dev", "Delivery"})

        self.assertEqual(errors, [("mswertz", "Nonexistent")])

    def test_passes_when_every_team_value_is_a_live_option(self):
        mapping = {"mswertz": "Dev", "hslh": "Delivery"}

        errors = validate_pr_triage_teams.find_unknown_team_values(mapping, valid_team_names={"Dev", "Delivery"})

        self.assertEqual(errors, [])


class FindDuplicateLoginsTest(unittest.TestCase):
    def test_flags_a_login_listed_twice(self):
        text = "teams:\n  mswertz: Dev\n  hslh: Delivery\n  mswertz: Delivery\n"

        duplicates = validate_pr_triage_teams.find_duplicate_logins(text)

        self.assertEqual(duplicates, ["mswertz"])

    def test_passes_when_every_login_appears_once(self):
        text = "teams:\n  mswertz: Dev\n  hslh: Delivery\n"

        duplicates = validate_pr_triage_teams.find_duplicate_logins(text)

        self.assertEqual(duplicates, [])


class ValidateBlankTeamValuesTest(unittest.TestCase):
    def test_flags_an_empty_team_value(self):
        mapping = {"someuser": ""}

        errors = validate_pr_triage_teams.find_blank_team_values(mapping)

        self.assertEqual(errors, ["someuser"])

    def test_flags_a_whitespace_only_team_value(self):
        mapping = {"someuser": "   "}

        errors = validate_pr_triage_teams.find_blank_team_values(mapping)

        self.assertEqual(errors, ["someuser"])

    def test_passes_a_non_blank_team_value(self):
        mapping = {"mswertz": "Dev"}

        errors = validate_pr_triage_teams.find_blank_team_values(mapping)

        self.assertEqual(errors, [])


class ValidateStatusOptionTest(unittest.TestCase):
    """find_missing_option is built on pr_triage_decide.find_option_id_by_name, the same
    lookup write time uses -- so a value missing here and a value missing at
    write time report the identical message."""

    def test_passes_when_working_is_a_live_status_option(self):
        status_options = [{"id": "47fc9ee4", "name": "\U0001f6e0️ Working"}]

        error = validate_pr_triage_teams.find_missing_option(status_options, "Working", strip_emoji=True)

        self.assertIsNone(error)

    def test_flags_when_working_is_not_a_live_status_option(self):
        status_options = [{"id": "98236657", "name": "✅ Done"}]

        error = validate_pr_triage_teams.find_missing_option(status_options, "Working", strip_emoji=True)

        self.assertIsNotNone(error)

    def test_passes_when_review_is_a_live_status_option(self):
        status_options = [{"id": "879449e7", "name": "\U0001f50d Review"}]

        error = validate_pr_triage_teams.find_missing_option(status_options, "Review", strip_emoji=True)

        self.assertIsNone(error)

    def test_flags_when_review_is_not_a_live_status_option(self):
        status_options = [{"id": "98236657", "name": "✅ Done"}]

        error = validate_pr_triage_teams.find_missing_option(status_options, "Review", strip_emoji=True)

        self.assertIsNotNone(error)


class ValidateTeamOptionTest(unittest.TestCase):
    def test_passes_when_dev_is_a_live_team_option(self):
        team_options = [{"id": "f2a5529c", "name": "Dev"}]

        error = validate_pr_triage_teams.find_missing_option(team_options, "Dev", strip_emoji=False)

        self.assertIsNone(error)

    def test_flags_when_dev_is_not_a_live_team_option(self):
        team_options = [{"id": "34b176a9", "name": "Delivery"}]

        error = validate_pr_triage_teams.find_missing_option(team_options, "Dev", strip_emoji=False)

        self.assertIsNotNone(error)


class MainWritesStepSummaryOnFailureTest(unittest.TestCase):
    def test_a_missing_mapping_file_still_writes_a_summary_and_exits_non_zero(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        with mock.patch.object(
            pr_triage, "load_teams_mapping", side_effect=FileNotFoundError("no such file: pr-triage-teams.yml")
        ):
            summary_text, exit_code = run_main(event)

        self.assertNotEqual(exit_code, 0)
        self.assertIsNotNone(exit_code)
        self.assertIn("### PR triage", summary_text)
        self.assertIn("no such file", summary_text)

    def test_assign_failure_skips_the_draft_flip_but_still_boards_review_and_exits_non_zero(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def refuse_draft_flip(url, token, method, body, query, variables):
            if "convertPullRequestToDraft" in query:
                raise AssertionError("assignment failure must skip the draft flip entirely")
            return None

        fake_http_request = make_fake_http_request(calls, extra=refuse_draft_flip)

        with mock.patch.object(pr_triage_github, "assign_author", side_effect=pr_triage_github.GraphqlError("boom")):
            summary_text, exit_code = run_main(event, http_request=fake_http_request, current_date=datetime.date(2026, 8, 8))

        self.assertNotEqual(exit_code, 0)
        self.assertIsNotNone(exit_code)

        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertTrue(any("addProjectV2ItemById" in query_of(call) for call in calls))

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 3)
        status_write = next(
            call for call in field_writes if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        )
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_OPT_REVIEW")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("boom", summary_text)
        self.assertIn("Board item", summary_text)
        self.assertIn("Status set", summary_text)
        self.assertIn("| Converted to draft | skipped, assignment failed |", summary_text)
        self.assertIn("Team set", summary_text)


class MainWiringTest(unittest.TestCase):
    def test_known_author_writes_go_to_the_right_endpoint_field_and_token(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []
        run_main(event, http_request=make_fake_http_request(calls), current_date=datetime.date(2026, 8, 8))

        assign_call = next(call for call in calls if call["url"].endswith("/assignees"))
        self.assertEqual(assign_call["token"], "github-token")

        draft_call = next(call for call in calls if "convertPullRequestToDraft" in query_of(call))
        self.assertEqual(draft_call["token"], "board-token")

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 3)

        status_write = field_writes[0]
        self.assertEqual(status_write["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_OPT_WORKING")
        self.assertEqual(status_write["token"], "board-token")

        team_write = field_writes[1]
        self.assertEqual(team_write["body"]["variables"]["fieldId"], pr_triage_github.TEAM_FIELD_ID)
        self.assertEqual(team_write["body"]["variables"]["optionId"], "TEAM_OPT_DEV")
        self.assertEqual(team_write["token"], "board-token")

        sprint_write = field_writes[2]
        self.assertEqual(sprint_write["body"]["variables"]["fieldId"], pr_triage_github.SPRINT_FIELD_ID)
        self.assertEqual(sprint_write["body"]["variables"]["iterationId"], "bd551114")
        self.assertEqual(sprint_write["token"], "board-token")


class MainRaisesOnDroppedAssignmentTest(unittest.TestCase):
    def test_dropped_assignment_skips_the_draft_flip_but_still_boards_review_and_exits_non_zero(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def refuse_draft_flip(url, token, method, body, query, variables):
            if "convertPullRequestToDraft" in query:
                raise AssertionError("a dropped assignment must skip the draft flip entirely")
            return None

        fake_http_request = make_fake_http_request(calls, assign_result={"assignees": []}, extra=refuse_draft_flip)

        summary_text, exit_code = run_main(event, http_request=fake_http_request, current_date=datetime.date(2026, 8, 8))

        self.assertNotEqual(exit_code, 0)
        self.assertIsNotNone(exit_code)

        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertTrue(any("addProjectV2ItemById" in query_of(call) for call in calls))

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 3)
        status_write = next(
            call for call in field_writes if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        )
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_OPT_REVIEW")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("mswertz", summary_text)
        self.assertIn("| Converted to draft | skipped, assignment failed |", summary_text)
        self.assertIn("Board item", summary_text)


class MainDraftFailureStillBoardsAndExitsNonZeroTest(unittest.TestCase):
    def test_forbidden_draft_conversion_does_not_prevent_assignment_or_board_writes(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def refuse_draft_flip(url, token, method, body, query, variables):
            if "convertPullRequestToDraft" in query:
                raise pr_triage_github.GraphqlError(
                    "GraphQL request returned errors: [{'type': 'FORBIDDEN', "
                    "'message': 'Resource not accessible by integration'}]"
                )
            return None

        fake_http_request = make_fake_http_request(calls, extra=refuse_draft_flip)

        summary_text, exit_code = run_main(event, http_request=fake_http_request, current_date=datetime.date(2026, 8, 8))

        self.assertNotEqual(exit_code, 0)
        self.assertIsNotNone(exit_code)

        assign_call = next(call for call in calls if call["url"].endswith("/assignees"))
        self.assertEqual(assign_call["token"], "github-token")

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 3)

        status_write = next(
            call for call in field_writes if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        )
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_OPT_REVIEW")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("| Assignee set | True |", summary_text)
        self.assertIn("FORBIDDEN", summary_text)
        self.assertIn("Board item", summary_text)
        self.assertIn("Status set", summary_text)
        self.assertIn("Team set", summary_text)
        self.assertIn("Sprint set", summary_text)


class MappingFilePathTest(unittest.TestCase):
    def test_is_absolute_even_when_file_argument_is_relative(self):
        result = pr_triage.mapping_file_path("some/relative/.github/scripts/pr_triage.py")

        self.assertTrue(os.path.isabs(result))

    def test_resolves_next_to_the_given_file_not_against_cwd(self):
        result = pr_triage.mapping_file_path("/opt/checkout/.github/scripts/pr_triage.py")

        self.assertEqual(result, "/opt/checkout/.github/pr-triage-teams.yml")


class MainWiringBoardUpdateTest(unittest.TestCase):
    def _run_board_update(
        self,
        action,
        is_draft,
        item_exists,
        current_status=None,
        current_team=None,
        current_sprint=None,
        mapping=None,
        author_login="mswertz",
    ):

        event = {
            "action": action,
            "pull_request": {
                "user": {"login": author_login},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": is_draft,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})

            if url.endswith("/assignees"):
                raise AssertionError("a board update must never touch the assignee")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("a board update must never touch the draft state")
            if "ReadyForReview" in query:
                raise AssertionError("a board update must never touch the draft state")

            if "projectItems" in query:
                if item_exists:
                    nodes = [
                        {
                            "id": "EXISTING_ITEM",
                            "project": {"id": pr_triage_github.BOARD_PROJECT_ID},
                            "status": {"name": current_status} if current_status else None,
                            "team": {"name": current_team} if current_team else None,
                            "sprint": {"title": current_sprint} if current_sprint else None,
                        }
                    ]
                else:
                    nodes = []
                return {"data": {"node": {"projectItems": {"nodes": nodes}}}}

            if "addProjectV2ItemById" in query:
                if item_exists:
                    raise AssertionError("must not add an item that already exists")
                return {"data": {"addProjectV2ItemById": {"item": {"id": "NEW_ITEM"}}}}

            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM"}}}}

            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage_github.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                if variables["fieldId"] == pr_triage_github.TEAM_FIELD_ID:
                    return {
                        "data": {
                            "node": {
                                "options": [
                                    {"id": "TEAM_DELIVERY_OPT", "name": "Delivery"},
                                    {"id": "TEAM_DEV_OPT", "name": "Dev"},
                                ]
                            }
                        }
                    }

            if "configuration" in query:
                return {
                    "data": {
                        "node": {
                            "configuration": {
                                "iterations": [
                                    {
                                        "id": "bd551114",
                                        "title": "Sprint 260",
                                        "startDate": "2026-08-03",
                                        "duration": 21,
                                    }
                                ]
                            }
                        }
                    }
                }

            raise AssertionError(f"unexpected call: {url} {query}")

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "PROJECT_BOARD_TOKEN": "board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "http_request", side_effect=fake_http_request
            ), mock.patch.object(
                pr_triage, "load_teams_mapping", return_value=mapping if mapping is not None else {"mswertz": "Dev"}
            ), mock.patch.object(
                pr_triage, "current_date", return_value=datetime.date(2026, 8, 8)
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        return calls, summary_text

    def _query_of(self, call):
        return (call["body"] or {}).get("query", "")

    def _field_writes(self, calls):
        return [call for call in calls if "updateProjectV2ItemFieldValue" in self._query_of(call)]

    def _assert_never_touches_assignee_or_draft(self, calls):
        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in self._query_of(call) for call in calls))

    # --- Status flips when it differs from the target and is still managed
    # (blank, Working, or Review); a non-managed value or an already-matching
    # one is left alone (decide_status returns None for both) ---

    def test_ready_for_review_overwrites_status_even_when_already_set(self):
        calls, _ = self._run_board_update(
            "ready_for_review", is_draft=False, item_exists=True, current_status="🛠️ Working", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(len(status_writes), 1)
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "879449e7")
        self.assertEqual(status_writes[0]["body"]["variables"]["itemId"], "EXISTING_ITEM")
        self.assertEqual(status_writes[0]["token"], "board-token")

    def test_converted_to_draft_overwrites_status_even_when_already_set(self):
        calls, _ = self._run_board_update(
            "converted_to_draft", is_draft=True, item_exists=True, current_status="🔍 Review", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_reopened_draft_overwrites_status_to_working(self):
        calls, _ = self._run_board_update(
            "reopened", is_draft=True, item_exists=True, current_status="🔍 Review", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_reopened_ready_overwrites_status_to_review(self):
        calls, _ = self._run_board_update(
            "reopened", is_draft=False, item_exists=True, current_status="🛠️ Working", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "879449e7")

    def test_transition_with_no_existing_item_adds_it(self):
        calls, _ = self._run_board_update("ready_for_review", is_draft=False, item_exists=False)

        add_item_calls = [call for call in calls if "addProjectV2ItemById" in self._query_of(call)]
        self.assertEqual(len(add_item_calls), 1)
        self.assertEqual(add_item_calls[0]["token"], "board-token")

    def test_transition_never_touches_a_non_managed_status(self):
        calls, _ = self._run_board_update(
            "ready_for_review", is_draft=False, item_exists=True, current_status="⛔️ Blocked", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes, [])

    # --- Team and Sprint fill only, never overwrite -- true for every action
    # in BOARD_UPDATE_ACTIONS, not just the transitions exercised here ---

    def test_transition_fills_team_when_empty(self):
        calls, _ = self._run_board_update(
            "ready_for_review",
            is_draft=False,
            item_exists=True,
            current_status="🛠️ Working",
            current_team=None,
            mapping={"mswertz": "Delivery"},
        )

        team_writes = [
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage_github.TEAM_FIELD_ID
        ]
        self.assertEqual(len(team_writes), 1)
        self.assertEqual(team_writes[0]["body"]["variables"]["optionId"], "TEAM_DELIVERY_OPT")

    def test_transition_never_overwrites_a_non_empty_team(self):
        calls, _ = self._run_board_update(
            "ready_for_review",
            is_draft=False,
            item_exists=True,
            current_status="🛠️ Working",
            current_team="Dev",
            mapping={"mswertz": "Delivery"},
        )

        team_writes = [
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage_github.TEAM_FIELD_ID
        ]
        self.assertEqual(team_writes, [])

    def test_transition_fills_sprint_when_empty(self):
        calls, _ = self._run_board_update(
            "converted_to_draft",
            is_draft=True,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint=None,
        )

        sprint_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.SPRINT_FIELD_ID
        ]
        self.assertEqual(len(sprint_writes), 1)
        self.assertEqual(sprint_writes[0]["body"]["variables"]["iterationId"], "bd551114")

    def test_transition_never_overwrites_a_non_empty_sprint(self):
        calls, _ = self._run_board_update(
            "converted_to_draft",
            is_draft=True,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint="Sprint 259",
        )

        sprint_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.SPRINT_FIELD_ID
        ]
        self.assertEqual(sprint_writes, [])

    # --- synchronize obeys the identical rule as the transitions: Status can
    # still flip (it is managed, not fill-only), Team and Sprint are fill-only ---

    def test_synchronize_item_already_exists_with_everything_set_writes_nothing(self):
        calls, summary_text = self._run_board_update(
            "synchronize",
            is_draft=False,
            item_exists=True,
            current_status="📋 Epic",
            current_team="Dev",
            current_sprint="Sprint 260",
        )

        self._assert_never_touches_assignee_or_draft(calls)
        self.assertEqual(self._field_writes(calls), [])
        self.assertFalse(any("addProjectV2ItemById" in self._query_of(call) for call in calls))
        self.assertIn("### PR triage", summary_text)

    def test_synchronize_fills_status_only_when_empty(self):
        calls, _ = self._run_board_update(
            "synchronize", is_draft=True, item_exists=True, current_status=None, current_team="Dev", current_sprint="Sprint 260"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(len(status_writes), 1)
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_synchronize_flips_a_managed_status_to_match_current_draft_state(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=True,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint="Sprint 260",
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(len(status_writes), 1)
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_synchronize_never_touches_a_non_managed_status(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=True,
            item_exists=True,
            current_status="📋 Epic",
            current_team="Dev",
            current_sprint="Sprint 260",
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes, [])

    def test_synchronize_fills_team_when_empty(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=False,
            item_exists=True,
            current_status="🔍 Review",
            current_team=None,
            current_sprint="Sprint 260",
            mapping={"mswertz": "Delivery"},
        )

        team_writes = [
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage_github.TEAM_FIELD_ID
        ]
        self.assertEqual(len(team_writes), 1)
        self.assertEqual(team_writes[0]["body"]["variables"]["optionId"], "TEAM_DELIVERY_OPT")

    def test_synchronize_never_overwrites_a_non_empty_team(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=False,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint="Sprint 260",
            mapping={"mswertz": "Delivery"},
        )

        team_writes = [
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage_github.TEAM_FIELD_ID
        ]
        self.assertEqual(team_writes, [])

    def test_synchronize_fills_sprint_when_empty(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=False,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint=None,
        )

        sprint_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.SPRINT_FIELD_ID
        ]
        self.assertEqual(len(sprint_writes), 1)
        self.assertEqual(sprint_writes[0]["body"]["variables"]["iterationId"], "bd551114")

    def test_synchronize_never_overwrites_a_non_empty_sprint(self):
        calls, _ = self._run_board_update(
            "synchronize",
            is_draft=False,
            item_exists=True,
            current_status="🔍 Review",
            current_team="Dev",
            current_sprint="Sprint 259",
        )

        sprint_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage_github.SPRINT_FIELD_ID
        ]
        self.assertEqual(sprint_writes, [])

    def test_synchronize_with_no_existing_item_adds_it_and_fills_all_three(self):
        calls, _ = self._run_board_update(
            "synchronize", is_draft=True, item_exists=False, mapping={"mswertz": "Delivery"}
        )

        add_item_calls = [call for call in calls if "addProjectV2ItemById" in self._query_of(call)]
        self.assertEqual(len(add_item_calls), 1)
        self.assertEqual(add_item_calls[0]["token"], "board-token")

        field_writes = self._field_writes(calls)
        written_field_ids = {call["body"]["variables"]["fieldId"] for call in field_writes}
        self.assertEqual(
            written_field_ids, {pr_triage_github.STATUS_FIELD_ID, pr_triage_github.TEAM_FIELD_ID, pr_triage_github.SPRINT_FIELD_ID}
        )
        for call in field_writes:
            self.assertEqual(call["body"]["variables"]["itemId"], "NEW_ITEM")
            self.assertEqual(call["token"], "board-token")


def _pr_event(action, is_draft, author="mswertz", number=1, node_id="PR_node"):
    return {
        "action": action,
        "pull_request": {
            "user": {"login": author},
            "head": {"ref": "feature/x"},
            "number": number,
            "node_id": node_id,
            "draft": is_draft,
        },
        "repository": {"full_name": "molgenis/molgenis-emx2"},
    }


class MainWiringClosingIssueTest(unittest.TestCase):
    """End-to-end (aim 6): a PR closing an issue redirects the board write
    from the PR's own card to each linked issue's, per ticket 05's
    acceptance criteria. `mswertz` maps to `Dev` in the real
    .github/pr-triage-teams.yml, same as every other opened-action test in
    this file."""

    def _field_writes(self, calls, item_id):
        return [
            call
            for call in calls
            if "updateProjectV2ItemFieldValue" in query_of(call) and call["body"]["variables"]["itemId"] == item_id
        ]

    def _add_calls(self, calls):
        return [call for call in calls if "addProjectV2ItemById" in query_of(call)]

    def _remove_calls(self, calls):
        """Calls to the removal mutation -- deleteProjectV2Item is GitHub's
        own name for it, kept in the GraphQL text; our identifiers say
        "remove"."""
        return [call for call in calls if "deleteProjectV2Item" in query_of(call)]

    # --- opened: assignee/draft run in full, board write redirects ---

    def test_opened_closing_a_boarded_issue_adds_no_pr_card_and_sets_only_status(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM_A", "status": "🔍 Review", "team": "Analysis", "sprint": "Sprint 259"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("opened", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertTrue(any(call["url"].endswith("/assignees") for call in calls))
        self.assertTrue(any("convertPullRequestToDraft" in query_of(call) for call in calls))

        self.assertEqual(self._add_calls(calls), [], "no card, for the PR or the issue, should be added")

        field_writes = self._field_writes(calls, "ISSUE_ITEM_A")
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")  # Working

    def test_opened_closing_an_unboarded_issue_boards_the_issue_with_all_three_fields(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_B", "number": 11}],
            issue_items={"ISSUE_B": None},
            add_item_ids={"ISSUE_B": "NEW_ISSUE_ITEM"},
        )

        run_main(_pr_event("opened", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        add_calls = self._add_calls(calls)
        self.assertEqual(len(add_calls), 1)
        self.assertEqual(add_calls[0]["body"]["variables"]["contentId"], "ISSUE_B")

        field_writes = self._field_writes(calls, "NEW_ISSUE_ITEM")
        written_field_ids = {call["body"]["variables"]["fieldId"] for call in field_writes}
        self.assertEqual(
            written_field_ids, {pr_triage_github.STATUS_FIELD_ID, pr_triage_github.TEAM_FIELD_ID, pr_triage_github.SPRINT_FIELD_ID}
        )
        team_write = next(c for c in field_writes if c["body"]["variables"]["fieldId"] == pr_triage_github.TEAM_FIELD_ID)
        self.assertEqual(team_write["body"]["variables"]["optionId"], "TEAM_DEV_OPT")

    def test_opened_removes_the_prs_existing_card(self):
        """F10: Auto-add (board 15's own built-in workflow) can put a card on
        the PR before this workflow's `opened` handler ever runs. That card
        must still be removed -- this was asserted by nothing before this
        test; every prior opened/transition removal test used pr_item=None."""
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM_A", "status": "🔍 Review", "team": "Analysis", "sprint": "Sprint 259"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "AUTO_ADDED_PR_ITEM", "status": None, "team": None, "sprint": None},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("opened", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "AUTO_ADDED_PR_ITEM")

    def test_unknown_author_closing_an_issue_skips_assignment_and_draft_but_still_redirects(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_B", "number": 11}],
            issue_items={"ISSUE_B": None},
            add_item_ids={"ISSUE_B": "NEW_ISSUE_ITEM"},
        )

        run_main(
            _pr_event("opened", is_draft=False, author="some-outside-contributor"),
            http_request=fake,
            current_date=datetime.date(2026, 8, 8),
        )

        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertEqual(self._add_calls(calls)[0]["body"]["variables"]["contentId"], "ISSUE_B")

    # --- the three transitions: Status only on an existing linked issue card ---

    def test_ready_for_review_moves_the_linked_issues_card_to_review(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        field_writes = self._field_writes(calls, "ISSUE_ITEM")
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "879449e7")  # Review

    def test_converted_to_draft_moves_the_linked_issues_card_to_working(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("converted_to_draft", is_draft=True), http_request=fake, current_date=datetime.date(2026, 8, 8))

        field_writes = self._field_writes(calls, "ISSUE_ITEM")
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")  # Working

    # --- F10: removal on `opened` and each transition, asserted individually.
    # A subTest loop over the three actions never varies `action` inside the
    # fake/assertions, so it cannot catch a mutant that only removes on one
    # specific action -- that is exactly the shape of the hole this closes. ---

    def test_ready_for_review_removes_the_prs_existing_card(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "PR_ITEM_RFR", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "PR_ITEM_RFR")

    def test_converted_to_draft_removes_the_prs_existing_card(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "PR_ITEM_CTD", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("converted_to_draft", is_draft=True), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "PR_ITEM_CTD")

    def test_reopened_removes_the_prs_existing_card(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "PR_ITEM_REOPENED", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("reopened", is_draft=True), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "PR_ITEM_REOPENED")

    def test_a_parked_linked_issue_is_not_moved(self):
        calls = []
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "⛔️ Blocked", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertEqual(self._field_writes(calls, "ISSUE_ITEM"), [])
        self.assertEqual(self._add_calls(calls), [])

    def test_a_pr_closing_two_issues_moves_both_and_still_gets_no_card(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}, {"id": "ISSUE_B", "number": 11}],
            issue_items={"ISSUE_A": None, "ISSUE_B": None},
            add_item_ids={"ISSUE_A": "NEW_A", "ISSUE_B": "NEW_B"},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        add_calls = self._add_calls(calls)
        self.assertEqual({c["body"]["variables"]["contentId"] for c in add_calls}, {"ISSUE_A", "ISSUE_B"})
        self.assertEqual(len(self._field_writes(calls, "NEW_A")), 3)
        self.assertEqual(len(self._field_writes(calls, "NEW_B")), 3)

    def test_a_pr_closing_two_already_boarded_issues_sets_status_only_on_both(self):
        """Review gap: the two-issue test above used two UNBOARDED issues,
        never exercising the existing-card path for more than one issue at
        once."""
        calls = []
        existing_a = {"id": "ISSUE_ITEM_A", "status": "🛠️ Working", "team": "Analysis", "sprint": "Sprint 259"}
        existing_b = {"id": "ISSUE_ITEM_B", "status": "🛠️ Working", "team": "Delivery", "sprint": "Sprint 259"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}, {"id": "ISSUE_B", "number": 11}],
            issue_items={"ISSUE_A": existing_a, "ISSUE_B": existing_b},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertEqual(self._add_calls(calls), [], "neither existing issue card should be re-added")

        for item_id in ("ISSUE_ITEM_A", "ISSUE_ITEM_B"):
            field_writes = self._field_writes(calls, item_id)
            self.assertEqual(len(field_writes), 1)
            self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
            self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "879449e7")  # Review

    # --- owner ruling: the PR's own card is removed LAST, only after every
    # linked issue's card has been written -- if an issue write fails
    # partway, the PR's card must survive (a transient duplicate is the
    # spec's explicitly preferred outcome over zero cards) ---

    def test_removal_happens_after_all_issue_writes_complete(self):
        """Pins the ORDER, not just the outcome: a mutant that removes the
        PR's card first and then writes the issue would still leave the
        final state correct (both eventually happen), so an order-blind test
        would not catch a regression back to remove-first."""
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "PR_ITEM_ORDER", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": None},
            add_item_ids={"ISSUE_A": "NEW_ISSUE_ITEM"},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_index = next(i for i, c in enumerate(calls) if "deleteProjectV2Item" in query_of(c))
        issue_write_indices = [
            i
            for i, c in enumerate(calls)
            if ("addProjectV2ItemById" in query_of(c) and c["body"]["variables"]["contentId"] == "ISSUE_A")
            or (
                "updateProjectV2ItemFieldValue" in query_of(c)
                and c["body"]["variables"]["itemId"] == "NEW_ISSUE_ITEM"
            )
        ]
        self.assertTrue(issue_write_indices, "expected at least the add call and one field write for the new issue item")
        self.assertTrue(
            all(i < remove_index for i in issue_write_indices),
            "every issue write must complete before the PR's card is removed",
        )

    def test_removal_never_happens_when_an_issue_write_fails(self):
        """The finding this reorder exists for: an issue write raising
        partway through must leave the PR's card in place -- a transient
        duplicate, not the zero-cards outcome the spec forbids."""
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item={"id": "PR_ITEM_SURVIVES", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": None},
            add_item_ids={"ISSUE_A": "NEW_ISSUE_ITEM"},
            status_options=[{"id": "SOME_OPT", "name": "NoMatchingOptionHere"}],
        )

        with self.assertRaises(ValueError):
            run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertEqual(self._remove_calls(calls), [], "the PR's card must survive an issue write that failed")

    # --- synchronize on a closing PR: writes NOTHING anywhere -- not the PR's
    # card (not even removal), not any issue's [owner ruling, fixes review
    # finding F1: a push must never leave zero cards for the same work] ---

    def test_synchronize_on_a_closing_pr_makes_no_write_and_no_removal_anywhere(self):
        calls = []
        pr_item = {"id": "PR_ITEM", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=pr_item,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": {"id": "ISSUE_ITEM", "status": None, "team": None, "sprint": None}},
        )

        run_main(_pr_event("synchronize", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertFalse(
            any(call["body"]["variables"].get("contentId") == "ISSUE_A" for call in calls if call["body"]),
            "synchronize must never even look up a linked issue's item",
        )
        self.assertEqual(self._field_writes(calls, "ISSUE_ITEM"), [])
        self.assertEqual(self._field_writes(calls, "PR_ITEM"), [])
        self.assertEqual(self._add_calls(calls), [])
        self.assertEqual(self._remove_calls(calls), [], "a push must never remove the PR's own card")

    def test_synchronize_f1_a_closing_pr_with_a_card_against_an_unboarded_issue_keeps_its_card(self):
        """Review finding F1: removing the PR's card on a push while never
        adding the issue's would leave zero cards for the same work. This is
        the exact scenario the fix (excluding synchronize from
        PR_CARD_REMOVAL_ACTIONS) closes."""
        calls = []
        pr_item = {"id": "PR_ITEM", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=pr_item,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": None},
            add_item_ids={"ISSUE_A": "NEW_ISSUE_ITEM"},
        )

        run_main(_pr_event("synchronize", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertEqual(self._remove_calls(calls), [], "the PR's own card must survive a synchronize")
        self.assertEqual(self._add_calls(calls), [], "the unboarded issue must not be added by synchronize either")

    # --- edited: link appears (remove the PR's card, never an issue's) ---

    def test_edited_link_appears_removes_the_prs_card_and_only_it(self):
        calls = []
        pr_item = {"id": "PR_ITEM_OLD", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        existing_issue_item = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=pr_item,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": existing_issue_item},
        )

        summary_text, _ = run_main(_pr_event("edited", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "PR_ITEM_OLD")
        self.assertEqual(self._field_writes(calls, "ISSUE_ITEM"), [], "edited never touches an existing issue card")

        self.assertIn("PR card removed", summary_text)
        self.assertIn("PR_ITEM_OLD", summary_text)
        self.assertIn("Review", summary_text)
        self.assertIn("Dev", summary_text)
        self.assertIn("Sprint 260", summary_text)

    def test_edited_adds_and_fills_a_missing_issue_card_but_leaves_status_unwritten(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_B", "number": 11}],
            issue_items={"ISSUE_B": None},
            add_item_ids={"ISSUE_B": "NEW_ISSUE"},
        )

        run_main(_pr_event("edited", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        self.assertEqual(self._add_calls(calls)[0]["body"]["variables"]["contentId"], "ISSUE_B")
        field_writes = self._field_writes(calls, "NEW_ISSUE")
        written_field_ids = {call["body"]["variables"]["fieldId"] for call in field_writes}
        self.assertEqual(written_field_ids, {pr_triage_github.TEAM_FIELD_ID, pr_triage_github.SPRINT_FIELD_ID})

    # --- edited: link disappears (the PR is boarded exactly like any other) ---

    def test_edited_link_disappears_reboards_the_pr_normally(self):
        calls = []
        fake = make_fake_http_request_for_redirect(calls, pr_item=None, closing_issues=[], add_item_ids={"PR_node": "NEW_PR_ITEM"})

        run_main(_pr_event("edited", is_draft=True), http_request=fake, current_date=datetime.date(2026, 8, 8))

        add_calls = self._add_calls(calls)
        self.assertEqual(len(add_calls), 1)
        self.assertEqual(add_calls[0]["body"]["variables"]["contentId"], "PR_node")
        self.assertEqual(self._remove_calls(calls), [])

        # Review gap: the re-add was asserted, but not that it is FILLED --
        # a mutant that adds an empty card survived without this.
        field_writes = self._field_writes(calls, "NEW_PR_ITEM")
        written_field_ids = {call["body"]["variables"]["fieldId"] for call in field_writes}
        self.assertEqual(
            written_field_ids, {pr_triage_github.STATUS_FIELD_ID, pr_triage_github.TEAM_FIELD_ID, pr_triage_github.SPRINT_FIELD_ID}
        )
        status_write = next(c for c in field_writes if c["body"]["variables"]["fieldId"] == pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(status_write["body"]["variables"]["optionId"], "47fc9ee4")  # Working, is_draft=True

    def test_add_remove_add_cycle_is_stable_not_one_shot(self):
        """The ticket calls this out explicitly: adding closes #N removes the
        PR's card, removing it re-boards and fills the PR, adding it again
        removes the NEW card too. Three independent edited events, each
        reflecting the board state the previous one would have left."""
        # Step 1: closes present, PR already has a card -> removed.
        calls_1 = []
        fake_1 = make_fake_http_request_for_redirect(
            calls_1,
            pr_item={"id": "PR_ITEM_1", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": {"id": "ISSUE_ITEM_A", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}},
        )
        run_main(_pr_event("edited", is_draft=False), http_request=fake_1, current_date=datetime.date(2026, 8, 8))
        self.assertEqual(len(self._remove_calls(calls_1)), 1)
        self.assertEqual(self._remove_calls(calls_1)[0]["body"]["variables"]["itemId"], "PR_ITEM_1")

        # Step 2: closes removed, PR has no card -> re-added and filled.
        calls_2 = []
        fake_2 = make_fake_http_request_for_redirect(
            calls_2, pr_item=None, closing_issues=[], add_item_ids={"PR_node": "PR_ITEM_2"}
        )
        run_main(_pr_event("edited", is_draft=False), http_request=fake_2, current_date=datetime.date(2026, 8, 8))
        add_calls_2 = self._add_calls(calls_2)
        self.assertEqual(len(add_calls_2), 1)
        self.assertEqual(add_calls_2[0]["body"]["variables"]["contentId"], "PR_node")
        self.assertEqual(self._remove_calls(calls_2), [])

        # Step 3: closes added again, PR now has the card step 2 created -> removed again.
        calls_3 = []
        fake_3 = make_fake_http_request_for_redirect(
            calls_3,
            pr_item={"id": "PR_ITEM_2", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"},
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": {"id": "ISSUE_ITEM_A", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}},
        )
        run_main(_pr_event("edited", is_draft=False), http_request=fake_3, current_date=datetime.date(2026, 8, 8))
        remove_calls_3 = self._remove_calls(calls_3)
        self.assertEqual(len(remove_calls_3), 1)
        self.assertEqual(remove_calls_3[0]["body"]["variables"]["itemId"], "PR_ITEM_2")

    # --- the mutant this ticket names explicitly: the removal call must
    # carry the PR's item id, never an issue's ---

    def test_removal_call_uses_the_prs_item_id_never_an_issues(self):
        calls = []
        pr_item = {"id": "PR_ITEM", "status": "🔍 Review", "team": "Dev", "sprint": "Sprint 260"}
        issue_item = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 260"}
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=pr_item,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            issue_items={"ISSUE_A": issue_item},
        )

        run_main(_pr_event("edited", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        remove_calls = self._remove_calls(calls)
        self.assertEqual(len(remove_calls), 1)
        self.assertEqual(remove_calls[0]["body"]["variables"]["itemId"], "PR_ITEM")
        self.assertNotEqual(remove_calls[0]["body"]["variables"]["itemId"], "ISSUE_ITEM")

    # --- a sidebar-only link never redirects (owner ruling, keyword-only) ---

    def test_a_sidebar_only_linked_issue_does_not_redirect_the_pr_gets_its_own_card(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_A", "number": 10}],
            user_linked_issues=[{"id": "ISSUE_A", "number": 10}],
            add_item_ids={"PR_node": "NEW_PR_ITEM"},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        add_calls = self._add_calls(calls)
        self.assertEqual(len(add_calls), 1)
        self.assertEqual(add_calls[0]["body"]["variables"]["contentId"], "PR_node")
        self.assertEqual(self._remove_calls(calls), [])

    def test_a_keyword_and_a_sidebar_link_together_redirect_on_the_keyword_one_only(self):
        calls = []
        fake = make_fake_http_request_for_redirect(
            calls,
            pr_item=None,
            closing_issues=[{"id": "ISSUE_KEYWORD", "number": 10}, {"id": "ISSUE_SIDEBAR", "number": 11}],
            user_linked_issues=[{"id": "ISSUE_SIDEBAR", "number": 11}],
            issue_items={"ISSUE_KEYWORD": None},
            add_item_ids={"ISSUE_KEYWORD": "NEW_ISSUE_ITEM"},
        )

        run_main(_pr_event("ready_for_review", is_draft=False), http_request=fake, current_date=datetime.date(2026, 8, 8))

        add_calls = self._add_calls(calls)
        self.assertEqual(len(add_calls), 1)
        self.assertEqual(add_calls[0]["body"]["variables"]["contentId"], "ISSUE_KEYWORD")


class BoardUpdateWritesStepSummaryOnFailureTest(unittest.TestCase):
    def test_step_summary_is_written_and_error_reraised_when_a_board_write_fails(self):

        event = {
            "action": "ready_for_review",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "PROJECT_BOARD_TOKEN": "board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage, "find_board_item_for_pr", side_effect=pr_triage_github.GraphqlError("board boom")
            ):
                with self.assertRaises(pr_triage_github.GraphqlError):
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("### PR triage", summary_text)
        self.assertIn("Failure", summary_text)
        self.assertIn("board boom", summary_text)

    def test_step_summary_is_still_written_when_the_teams_mapping_fails_to_load(self):
        """Review finding: handle_board_update used to load the teams mapping
        OUTSIDE its try/finally, so a mapping-read failure on a
        transition/synchronize/edited event wrote no step summary at all --
        unlike the opened path (MainWritesStepSummaryOnFailureTest), which
        already handled this gracefully and had a test for it."""
        event = {
            "action": "ready_for_review",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "PROJECT_BOARD_TOKEN": "board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage, "load_teams_mapping", side_effect=FileNotFoundError("no such file: pr-triage-teams.yml")
            ):
                with self.assertRaises(FileNotFoundError):
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("### PR triage", summary_text)
        self.assertIn("Failure", summary_text)
        self.assertIn("no such file", summary_text)


class BoardUpdateResolvesFieldsBeforeAddingBoardItemTest(unittest.TestCase):
    def test_fields_are_resolved_before_a_missing_item_is_added(self):

        event = {
            "action": "ready_for_review",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        call_kinds = []

        def fake_http_request(url, token, method="GET", body=None):
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if "projectItems" in query:
                call_kinds.append("find_item")
                return {"data": {"node": {"projectItems": {"nodes": []}}}}
            if "addProjectV2ItemById" in query:
                call_kinds.append("add_item")
                return {"data": {"addProjectV2ItemById": {"item": {"id": "NEW_ITEM"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                call_kinds.append("set_field")
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "NEW_ITEM"}}}}
            if "options { id name }" in query:
                call_kinds.append("fetch_options")
                if variables["fieldId"] == pr_triage_github.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                return {"data": {"node": {"options": [{"id": "TEAM_DEV_OPT", "name": "Dev"}]}}}
            if "configuration" in query:
                call_kinds.append("fetch_iterations")
                return {
                    "data": {
                        "node": {
                            "configuration": {
                                "iterations": [
                                    {"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}
                                ]
                            }
                        }
                    }
                }
            raise AssertionError(f"unexpected call: {url} {query}")

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "PROJECT_BOARD_TOKEN": "board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "http_request", side_effect=fake_http_request
            ), mock.patch.object(pr_triage, "load_teams_mapping", return_value={"mswertz": "Dev"}), mock.patch.object(
                pr_triage, "current_date", return_value=datetime.date(2026, 8, 8)
            ):
                pr_triage.main()

        add_item_index = call_kinds.index("add_item")
        self.assertLess(call_kinds.index("fetch_options"), add_item_index)

    def test_a_status_resolution_failure_never_adds_a_board_item(self):

        event = {
            "action": "ready_for_review",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        def fake_http_request(url, token, method="GET", body=None):
            query = (body or {}).get("query", "")
            if "projectItems" in query:
                return {"data": {"node": {"projectItems": {"nodes": []}}}}
            if "addProjectV2ItemById" in query:
                raise AssertionError("must not add a board item when Status resolution fails")
            if "options { id name }" in query:
                return {"data": {"node": {"options": [{"id": "SOME_OPT", "name": "NoMatchingOptionHere"}]}}}
            if "configuration" in query:
                return {"data": {"node": {"configuration": {"iterations": []}}}}
            raise AssertionError(f"unexpected call: {url} {query}")

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "PROJECT_BOARD_TOKEN": "board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "http_request", side_effect=fake_http_request
            ), mock.patch.object(pr_triage, "load_teams_mapping", return_value={"mswertz": "Dev"}), mock.patch.object(
                pr_triage, "current_date", return_value=datetime.date(2026, 8, 8)
            ):
                with self.assertRaises(ValueError):
                    pr_triage.main()


class MainWiringUnknownAuthorTest(unittest.TestCase):
    def _run_main_for(self, is_draft):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "some-outside-contributor"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": is_draft,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def forbid_assign_or_draft(url, token, method, body, query, variables):
            if url.endswith("/assignees"):
                raise AssertionError("unknown author must never be assigned")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("unknown author's draft state must never be touched")
            return None

        fake_http_request = make_fake_http_request(
            calls, team_options=[{"id": "TEAM_DEV_OPT", "name": "Dev"}], extra=forbid_assign_or_draft
        )

        summary_text, _ = run_main(event, http_request=fake_http_request, current_date=datetime.date(2026, 8, 8))

        return calls, summary_text

    def _assert_unknown_author_boarded_correctly(self, calls, summary_text, expected_status_option_id):
        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 3)

        status_write = field_writes[0]
        self.assertEqual(status_write["body"]["variables"]["fieldId"], pr_triage_github.STATUS_FIELD_ID)
        self.assertEqual(status_write["body"]["variables"]["optionId"], expected_status_option_id)
        self.assertEqual(status_write["token"], "board-token")

        team_write = field_writes[1]
        self.assertEqual(team_write["body"]["variables"]["fieldId"], pr_triage_github.TEAM_FIELD_ID)
        self.assertEqual(team_write["body"]["variables"]["optionId"], "TEAM_DEV_OPT")
        self.assertEqual(team_write["token"], "board-token")

        sprint_write = field_writes[2]
        self.assertEqual(sprint_write["body"]["variables"]["fieldId"], pr_triage_github.SPRINT_FIELD_ID)
        self.assertEqual(sprint_write["body"]["variables"]["iterationId"], "bd551114")
        self.assertEqual(sprint_write["token"], "board-token")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("| Assignee set | skipped, unknown author is never assigned |", summary_text)
        self.assertIn(
            "| Converted to draft | skipped, draft state left untouched for unknown author |", summary_text
        )
        self.assertIn("absent from .github/pr-triage-teams.yml", summary_text)

    def test_unknown_author_ready_for_review_is_boarded_review(self):
        calls, summary_text = self._run_main_for(is_draft=False)
        self._assert_unknown_author_boarded_correctly(calls, summary_text, "STATUS_OPT_REVIEW")

    def test_unknown_author_already_draft_is_boarded_working(self):
        calls, summary_text = self._run_main_for(is_draft=True)
        self._assert_unknown_author_boarded_correctly(calls, summary_text, "STATUS_OPT_WORKING")


class MainResolvesOptionsBeforeAddingBoardItemTest(unittest.TestCase):
    def test_status_and_team_options_are_resolved_before_the_item_is_added(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []
        run_main(event, http_request=make_fake_http_request(calls), current_date=datetime.date(2026, 8, 8))

        def kind_of(call):
            query = query_of(call)
            if "options { id name }" in query:
                variables = (call["body"] or {}).get("variables", {})
                return "fetch_status_options" if variables["fieldId"] == pr_triage_github.STATUS_FIELD_ID else "fetch_team_options"
            if "configuration" in query:
                return "fetch_iterations"
            if "addProjectV2ItemById" in query:
                return "add_item"
            return None

        call_kinds = [kind_of(call) for call in calls]
        add_item_index = call_kinds.index("add_item")
        self.assertLess(call_kinds.index("fetch_status_options"), add_item_index)
        self.assertLess(call_kinds.index("fetch_team_options"), add_item_index)
        self.assertLess(call_kinds.index("fetch_iterations"), add_item_index)

    def test_a_resolution_failure_never_adds_a_board_item(self):
        event = {
            "action": "opened",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def refuse_add_item(url, token, method, body, query, variables):
            if "addProjectV2ItemById" in query:
                raise AssertionError("must not add a board item when option resolution fails")
            return None

        fake_http_request = make_fake_http_request(
            calls,
            status_options=[{"id": "SOME_OPT", "name": "NoMatchingOptionHere"}],
            team_options=[{"id": "SOME_OPT", "name": "NoMatchingOptionHere"}],
            extra=refuse_add_item,
        )

        summary_text, exit_code = run_main(event, http_request=fake_http_request, current_date=datetime.date(2026, 8, 8))

        self.assertNotEqual(exit_code, 0)
        self.assertIsNotNone(exit_code)
        self.assertFalse(any("addProjectV2ItemById" in query_of(call) for call in calls))


class MainIgnoresUnrecognizedActionTest(unittest.TestCase):
    def test_an_unrecognized_action_makes_no_writes_and_reads_no_mapping_file(self):

        event = {
            "action": "labeled",
            "pull_request": {
                "user": {"login": "mswertz"},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": False,
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        def fake_http_request(url, token, method="GET", body=None):
            raise AssertionError(f"an unrecognized action must make no API call, got: {url}")

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {"GITHUB_EVENT_PATH": event_path, "GITHUB_STEP_SUMMARY": summary_path}

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "http_request", side_effect=fake_http_request
            ), mock.patch.object(
                pr_triage, "load_teams_mapping", side_effect=AssertionError("must not read the mapping file")
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("labeled", summary_text)
        self.assertIn("no action", summary_text.lower())


class FindBoardItemForPrTest(unittest.TestCase):
    """find_board_item_for_pr now returns {"item", "closing_issues"} rather
    than the item alone -- ticket 05 rides closingIssuesReferences on this
    same query. See ClosingIssuesReferencesTest below for that half; these
    fixtures omit the field entirely (as an Issue-fragment response would),
    proving the parser tolerates its absence."""

    def test_returns_the_matching_project_item_with_its_current_status_team_and_sprint(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "OTHER_ITEM",
                                "project": {"id": "PVT_someOtherBoard"},
                                "status": {"name": "Done"},
                                "team": {"name": "Analysis"},
                                "sprint": {"title": "Sprint 100"},
                            },
                            {
                                "id": "EXISTING_ITEM",
                                "project": {"id": pr_triage_github.BOARD_PROJECT_ID},
                                "status": {"name": "🛠️ Working"},
                                "team": {"name": "Dev"},
                                "sprint": {"title": "Sprint 260"},
                            },
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        item = result["item"]
        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertEqual(item["status"], "🛠️ Working")
        self.assertEqual(item["team"], "Dev")
        self.assertEqual(item["sprint"], "Sprint 260")
        self.assertEqual(result["closing_issues"], [])

    def test_returns_none_when_pr_has_no_item_on_this_board(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "OTHER_ITEM",
                                "project": {"id": "PVT_someOtherBoard"},
                                "status": {"name": "Done"},
                                "team": {"name": "Analysis"},
                                "sprint": {"title": "Sprint 100"},
                            }
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(result["item"])

    def test_returns_none_when_pr_has_no_items_at_all(self):
        response = {"data": {"node": {"projectItems": {"nodes": []}}}}
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(result["item"])

    def test_status_team_and_sprint_are_none_when_the_fields_are_unset(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "EXISTING_ITEM",
                                "project": {"id": pr_triage_github.BOARD_PROJECT_ID},
                                "status": None,
                                "team": None,
                                "sprint": None,
                            }
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        item = result["item"]
        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertIsNone(item["status"])
        self.assertIsNone(item["team"])
        self.assertIsNone(item["sprint"])

    def test_find_board_item_for_pr_raises_naming_the_pr_node_id_when_node_is_null(self):
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": {"node": None}}):
            with self.assertRaises(pr_triage_github.GraphqlError) as context:
                pr_triage.find_board_item_for_pr("STALE_PR_NODE_ID", "board-token")

        self.assertIn("STALE_PR_NODE_ID", str(context.exception))


class ClosingIssuesReferencesTest(unittest.TestCase):
    """closingIssuesReferences (both the unfiltered and userLinkedOnly=true
    variants) rides the same node(id:) query as projectItems -- one round
    trip, see notes/github-facts.md §7-8 for the live probes. The returned
    closing_issues is already the KEYWORD-derived set (keyword_closing_issues
    applied); KeywordClosingIssuesTest above covers the subtraction itself."""

    def test_returns_the_closing_issues_when_present_and_none_are_user_linked(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {"nodes": []},
                    "closingIssuesReferences": {"nodes": [{"id": "ISSUE_A", "number": 42}]},
                    "userLinkedClosingIssues": {"nodes": []},
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(result["closing_issues"], [{"id": "ISSUE_A", "number": 42}])

    def test_returns_all_linked_issues_for_a_pr_closing_more_than_one(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {"nodes": []},
                    "closingIssuesReferences": {
                        "nodes": [{"id": "ISSUE_A", "number": 42}, {"id": "ISSUE_B", "number": 43}]
                    },
                    "userLinkedClosingIssues": {"nodes": []},
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(result["closing_issues"], [{"id": "ISSUE_A", "number": 42}, {"id": "ISSUE_B", "number": 43}])

    def test_a_sidebar_only_link_is_excluded(self):
        """all == userLinked, exactly as the live probe recorded for a
        sidebar-only PR (notes/github-facts.md §8, PR #6602)."""
        response = {
            "data": {
                "node": {
                    "projectItems": {"nodes": []},
                    "closingIssuesReferences": {"nodes": [{"id": "ISSUE_A", "number": 42}]},
                    "userLinkedClosingIssues": {"nodes": [{"id": "ISSUE_A", "number": 42}]},
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(result["closing_issues"], [])

    def test_empty_list_when_the_field_is_present_but_empty(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {"nodes": []},
                    "closingIssuesReferences": {"nodes": []},
                    "userLinkedClosingIssues": {"nodes": []},
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(result["closing_issues"], [])

    def test_empty_list_when_the_field_is_absent_entirely_as_an_issue_node_would_be(self):
        response = {"data": {"node": {"projectItems": {"nodes": []}}}}
        with mock.patch.object(pr_triage_github, "http_request", return_value=response):
            result = pr_triage.find_board_item_for_pr("ISSUE_node", "board-token")

        self.assertEqual(result["closing_issues"], [])

    def test_the_query_asks_for_projectitems_and_both_closing_issue_variants_in_one_call(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {"nodes": []},
                    "closingIssuesReferences": {"nodes": []},
                    "userLinkedClosingIssues": {"nodes": []},
                }
            }
        }
        with mock.patch.object(pr_triage_github, "http_request", return_value=response) as mock_request:
            pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(mock_request.call_count, 1)
        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertIn("projectItems", called_body["query"])
        self.assertIn("closingIssuesReferences", called_body["query"])
        self.assertIn("userLinkedOnly: true", called_body["query"])


class IsCredentialErrorTest(unittest.TestCase):
    def test_401_is_a_credential_error(self):
        self.assertTrue(validate_pr_triage_teams.is_credential_error(_FakeHttpError(401)))

    def test_403_is_a_credential_error(self):
        self.assertTrue(validate_pr_triage_teams.is_credential_error(_FakeHttpError(403)))

    def test_500_is_not_a_credential_error(self):
        self.assertFalse(validate_pr_triage_teams.is_credential_error(_FakeHttpError(500)))


class _FakeHttpError(Exception):
    def __init__(self, code):
        super().__init__(f"HTTP {code}")
        self.code = code


class FindBannedTermsInSourceTest(unittest.TestCase):
    def test_flags_author_association(self):
        source_text = "if: github.event.pull_request.author_association == 'OWNER'\n"

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, ["author_association"])

    def test_flags_user_type(self):
        source_text = "if pull_request.user.type == 'Bot':\n    pass\n"

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, ["user.type"])

    def test_flags_collaborators_endpoint(self):
        source_text = "url = f'{GITHUB_API_URL}/repos/{repo}/collaborators/{login}/permission'\n"

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, ["/collaborators"])

    def test_flags_double_quoted_python_user_type_subscript(self):
        source_text = 'author_type = pull_request["user"]["type"]\n'

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, ['["user"]["type"]'])

    def test_flags_single_quoted_python_user_type_subscript(self):
        source_text = "author_type = pull_request['user']['type']\n"

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, ["['user']['type']"])

    def test_passes_source_without_banned_terms(self):
        source_text = "on:\n  pull_request:\n    types: [opened, reopened]\n"

        found = validate_pr_triage_teams.find_banned_terms_in_source(source_text)

        self.assertEqual(found, [])


class FindBannedTermsAcrossTriageSourceTest(unittest.TestCase):
    def _write_clean_triage_source(self, tmp_dir):
        scripts_dir = os.path.join(tmp_dir, ".github", "scripts")
        workflows_dir = os.path.join(tmp_dir, ".github", "workflows")
        os.makedirs(scripts_dir)
        os.makedirs(workflows_dir)
        with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
            handle.write("on:\n  pull_request:\n    types: [opened, reopened]\n")
        with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
            handle.write("def decide():\n    pass\n")
        with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
            handle.write("BANNED = ('author_association',)\n")
        with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
            handle.write("workflow_text = 'author_association'\n")
        return scripts_dir, workflows_dir

    def test_flags_a_banned_term_planted_in_a_script_file(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            scripts_dir, _ = self._write_clean_triage_source(tmp_dir)
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("if pull_request.author_association == 'OWNER':\n    pass\n")

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [("pr_triage.py", ["author_association"])])

    def test_flags_a_banned_term_planted_in_the_workflow_file_itself(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            _, workflows_dir = self._write_clean_triage_source(tmp_dir)
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("if: github.event.pull_request.author_association == 'OWNER'\n")

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [("pr-triage.yml", ["author_association"])])

    def test_passes_clean_triage_source(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            self._write_clean_triage_source(tmp_dir)

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [])


class ValidateMainExitsNonZeroOnDuplicateLoginTest(unittest.TestCase):
    def test_main_exits_non_zero_when_a_login_is_listed_twice(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = os.path.join(tmp_dir, "repo")
            scripts_dir = os.path.join(repo_root, ".github", "scripts")
            workflows_dir = os.path.join(repo_root, ".github", "workflows")
            os.makedirs(scripts_dir)
            os.makedirs(workflows_dir)
            with open(os.path.join(repo_root, ".github", "pr-triage-teams.yml"), "w", encoding="utf-8") as handle:
                handle.write("teams:\n  mswertz: Dev\n  mswertz: Delivery\n")
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("on:\n  pull_request:\n    types: [opened]\n")
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("def decide():\n    pass\n")
            with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
                handle.write("BANNED = ('author_association',)\n")
            with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("workflow_text = 'author_association'\n")

            env = {"PROJECT_BOARD_TOKEN": "board-token"}
            fake_status_options = [{"id": "s1", "name": "🛠️ Working"}, {"id": "s2", "name": "🔍 Review"}]
            fake_team_options = [{"id": "t1", "name": "Dev"}, {"id": "t2", "name": "Delivery"}]

            def fake_fetch(field_id, token):
                if field_id == pr_triage_github.TEAM_FIELD_ID:
                    return fake_team_options
                return fake_status_options

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "fetch_project_field_options", side_effect=fake_fetch
            ):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class ValidateMainChecksStatusConstantsResolveLiveTest(unittest.TestCase):
    def _write_clean_repo(self, tmp_dir):
        repo_root = os.path.join(tmp_dir, "repo")
        scripts_dir = os.path.join(repo_root, ".github", "scripts")
        workflows_dir = os.path.join(repo_root, ".github", "workflows")
        os.makedirs(scripts_dir)
        os.makedirs(workflows_dir)
        with open(os.path.join(repo_root, ".github", "pr-triage-teams.yml"), "w", encoding="utf-8") as handle:
            handle.write("teams:\n  mswertz: Dev\n")
        with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
            handle.write("on:\n  pull_request:\n    types: [opened]\n")
        with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
            handle.write("def decide():\n    pass\n")
        with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
            handle.write("BANNED = ('author_association',)\n")
        with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
            handle.write("workflow_text = 'author_association'\n")
        return repo_root

    def test_validator_flags_a_status_review_that_diverged_from_the_unknown_author_status(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = self._write_clean_repo(tmp_dir)

            env = {"PROJECT_BOARD_TOKEN": "board-token"}
            fake_status_options = [{"id": "s1", "name": "🛠️ Working"}, {"id": "s2", "name": "🔍 Review"}]
            fake_team_options = [{"id": "t1", "name": "Dev"}]

            def fake_fetch(field_id, token):
                if field_id == pr_triage_github.TEAM_FIELD_ID:
                    return fake_team_options
                return fake_status_options

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "fetch_project_field_options", side_effect=fake_fetch
            ), mock.patch.object(pr_triage_decide, "STATUS_REVIEW", "DivergedFromUnknownAuthorStatus"):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class ValidateMainExitsNonZeroOnBannedTermTest(unittest.TestCase):
    def test_main_exits_non_zero_when_a_banned_term_is_planted(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = os.path.join(tmp_dir, "repo")
            scripts_dir = os.path.join(repo_root, ".github", "scripts")
            workflows_dir = os.path.join(repo_root, ".github", "workflows")
            os.makedirs(scripts_dir)
            os.makedirs(workflows_dir)
            with open(os.path.join(repo_root, ".github", "pr-triage-teams.yml"), "w", encoding="utf-8") as handle:
                handle.write("teams:\n  mswertz: Dev\n")
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("if: github.event.pull_request.author_association == 'OWNER'\n")
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("def decide():\n    pass\n")
            with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
                handle.write("BANNED = ('author_association',)\n")
            with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("workflow_text = 'author_association'\n")

            env = {"PROJECT_BOARD_TOKEN": "board-token"}
            fake_options = [{"id": "opt", "name": "Dev"}]

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage_github, "fetch_project_field_options", return_value=fake_options
            ):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class ValidateMainRunsOfflineWithoutBoardTokenTest(unittest.TestCase):
    def test_offline_checks_pass_green_with_a_clean_mapping_and_no_token_in_the_environment(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = os.path.join(tmp_dir, "repo")
            scripts_dir = os.path.join(repo_root, ".github", "scripts")
            workflows_dir = os.path.join(repo_root, ".github", "workflows")
            os.makedirs(scripts_dir)
            os.makedirs(workflows_dir)
            with open(os.path.join(repo_root, ".github", "pr-triage-teams.yml"), "w", encoding="utf-8") as handle:
                handle.write("teams:\n  mswertz: Dev\n")
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("on:\n  pull_request:\n    types: [opened]\n")
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("def decide():\n    pass\n")
            with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
                handle.write("BANNED = ('author_association',)\n")
            with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("workflow_text = 'author_association'\n")

            with mock.patch.dict(os.environ, {}, clear=True), mock.patch.object(
                pr_triage_github,
                "fetch_project_field_options",
                side_effect=AssertionError("offline validation must never call the live board"),
            ):
                validate_pr_triage_teams.main(repo_root=repo_root)

    def test_a_genuine_mapping_defect_still_fails_with_no_token_present(self):

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = os.path.join(tmp_dir, "repo")
            scripts_dir = os.path.join(repo_root, ".github", "scripts")
            workflows_dir = os.path.join(repo_root, ".github", "workflows")
            os.makedirs(scripts_dir)
            os.makedirs(workflows_dir)
            with open(os.path.join(repo_root, ".github", "pr-triage-teams.yml"), "w", encoding="utf-8") as handle:
                handle.write("teams:\n  dependabot[bot]: Dev\n")
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("on:\n  pull_request:\n    types: [opened]\n")
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("def decide():\n    pass\n")
            with open(os.path.join(scripts_dir, "validate_pr_triage_teams.py"), "w", encoding="utf-8") as handle:
                handle.write("BANNED = ('author_association',)\n")
            with open(os.path.join(scripts_dir, "test_pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("workflow_text = 'author_association'\n")

            with mock.patch.dict(os.environ, {}, clear=True):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class WorkflowIncludesEditedInPullRequestTypesTest(unittest.TestCase):
    """Ticket 05: the link can appear or disappear after open, so `edited`
    must be seen the moment it happens, not at the next push."""

    def test_edited_is_a_triggering_type_and_reaches_the_board_update_handler(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        types_line = next(line for line in workflow_text.splitlines() if "types:" in line)
        self.assertIn("edited", types_line)

        self.assertIn("edited", pr_triage_decide.BOARD_UPDATE_ACTIONS)


class WorkflowHasAPerPrConcurrencyGroupTest(unittest.TestCase):
    def test_concurrency_group_is_scoped_per_pr_and_does_not_cancel_in_progress_runs(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        self.assertIn("concurrency:", workflow_text)
        self.assertIn("github.event.pull_request.number", workflow_text)
        self.assertNotIn("cancel-in-progress: true", workflow_text)


class WorkflowGithubTokenPermissionsTest(unittest.TestCase):
    """Assigning a PULL REQUEST needs pull-requests:write as well as issues:write.

    This assertion was once the exact opposite: it pinned the narrower block,
    on the reasoning that assignment goes through an /issues/ endpoint so only
    issues:write could be required. A real run disproved it — the assignees
    call returned 403 "Resource not accessible by integration". The requirement
    changed, so the check changed with it. issues:write alone is observed to
    fail; this pair is observed to work; pull-requests:write alone is untried.
    """

    def test_triage_job_grants_both_permissions_assignment_needs(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        self.assertIn("issues: write", workflow_text)
        self.assertIn("pull-requests: write", workflow_text)


class WorkflowOnlyTriagesPrsBasedOnTheDefaultBranchTest(unittest.TestCase):
    def test_triage_job_guards_on_base_ref_matching_the_default_branch(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        jobs = workflow_text.split("\n  triage:", 1)
        self.assertEqual(len(jobs), 2, "expected exactly one 'triage:' job in the workflow")
        validate_text, triage_text = jobs

        self.assertNotIn("base.ref", validate_text)
        self.assertNotIn("default_branch", validate_text)

        self.assertIn("github.event.pull_request.base.ref", triage_text)
        self.assertIn("github.event.repository.default_branch", triage_text)
        self.assertNotIn("== 'master'", triage_text)
        self.assertNotIn('== "master"', triage_text)


class WorkflowValidateJobHoldsNoBoardTokenTest(unittest.TestCase):
    def test_validate_job_env_has_no_project_board_token(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        jobs = workflow_text.split("\n  triage:", 1)
        self.assertEqual(len(jobs), 2, "expected exactly one 'triage:' job in the workflow")
        validate_text, _ = jobs

        self.assertNotIn("PROJECT_BOARD_TOKEN", validate_text)


class WorkflowRunUnitTestsStepDiscoversEveryTestFileTest(unittest.TestCase):
    """The split (ticket 06) turned one test file into three, and a run
    command that once worked -- `python3 .github/scripts/test_pr_triage.py`
    -- now silently drops 60 of 167 tests, because unittest.main() only
    loads TestCase classes defined in __main__ itself. The fix is a
    `unittest discover` invocation, but discover has its own silent-drop
    failure mode: a `-p` pattern that does not match every test*.py file
    actually present in the directory skips whatever it misses, and still
    prints OK. This pins the REQUIREMENT, not one exact command string --
    a differently-spelled discover invocation that still satisfies it must
    stay green."""

    def test_run_unit_tests_step_is_a_discover_invocation_whose_pattern_matches_every_test_file(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        step_marker = "- name: Run unit tests"
        self.assertIn(step_marker, workflow_text, "expected a 'Run unit tests' step in the workflow")
        after_step = workflow_text.split(step_marker, 1)[1]
        run_line = next(line for line in after_step.splitlines() if "run:" in line)
        run_command = run_line.split("run:", 1)[1].strip()

        # Tokenise rather than match one exact spelling -- a `-v` for debugging,
        # a `-t` start-directory, or `-s`/`-p` in the other order are all still
        # valid discover invocations and must not go red for being spelled
        # differently.
        tokens = shlex.split(run_command)
        try:
            m_index = tokens.index("-m")
            unittest_index = tokens.index("unittest")
            discover_index = tokens.index("discover")
            is_discover_invocation = m_index < unittest_index < discover_index
        except ValueError:
            is_discover_invocation = False
        self.assertTrue(
            is_discover_invocation,
            f"'Run unit tests' step must be a `unittest discover` invocation, got: {run_command!r}",
        )

        def value_after(flags):
            for index, token in enumerate(tokens):
                if token in flags and index + 1 < len(tokens):
                    return tokens[index + 1]
            return None

        discover_dir = value_after({"-s", "--start-directory"})
        pattern = value_after({"-p", "--pattern"}) or "test*.py"

        self.assertIsNotNone(discover_dir, f"no -s/--start-directory found in: {run_command!r}")
        scripts_dir = os.path.join(repo_root, ".github", "scripts")
        self.assertEqual(os.path.join(repo_root, discover_dir), scripts_dir)

        test_files = [name for name in os.listdir(scripts_dir) if name.startswith("test") and name.endswith(".py")]
        self.assertTrue(test_files, "expected at least one test*.py file in .github/scripts")
        unmatched = [name for name in test_files if not fnmatch.fnmatch(name, pattern)]
        self.assertEqual(
            unmatched, [], f"pattern {pattern!r} does not match every test*.py file present: {unmatched}"
        )


if __name__ == "__main__":
    unittest.main()
