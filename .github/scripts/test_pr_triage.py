import json
import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage
import validate_pr_triage_teams


class DecidePrTriageTest(unittest.TestCase):
    def test_known_author_is_assigned_drafted_and_boarded_working(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="mswertz", is_draft=False, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertTrue(decision["assign"])
        self.assertTrue(decision["force_draft"])
        self.assertEqual(decision["status"], "Working")
        self.assertEqual(decision["team"], "Dev")

    def test_known_author_mapped_to_delivery_boards_under_delivery(self):
        mapping = {"hslh": "Delivery"}

        decision = pr_triage.decide(author_login="hslh", is_draft=True, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertEqual(decision["team"], "Delivery")

    def test_unknown_author_is_boarded_review_dev_without_assign_or_draft(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="some-outside-contributor", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["status"], "Review")
        self.assertEqual(decision["team"], "Dev")

    def test_unknown_author_already_draft_is_still_not_touched(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="some-outside-contributor", is_draft=True, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["status"], "Review")
        self.assertEqual(decision["team"], "Dev")

    def test_bot_author_is_unknown_regardless_of_login_shape(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="dependabot[bot]", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
        self.assertEqual(decision["status"], "Review")
        self.assertEqual(decision["team"], "Dev")

    def test_known_author_already_draft_is_not_re_converted(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="mswertz", is_draft=True, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertFalse(decision["force_draft"])

    def test_known_author_ready_for_review_is_converted_to_draft(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="mswertz", is_draft=False, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertTrue(decision["force_draft"])

    def test_empty_team_value_is_treated_as_unknown(self):
        mapping = {"someuser": ""}

        decision = pr_triage.decide(author_login="someuser", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["status"], "Review")
        self.assertEqual(decision["team"], "Dev")

    def test_whitespace_only_team_value_is_treated_as_unknown(self):
        mapping = {"someuser": "   "}

        decision = pr_triage.decide(author_login="someuser", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])

    def test_login_matching_is_exact_case_and_can_only_ever_miss(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="Mswertz", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])


class DecideTransitionTest(unittest.TestCase):
    def test_ready_for_review_moves_status_to_review(self):
        transition = pr_triage.decide_transition("ready_for_review")

        self.assertEqual(transition["status"], pr_triage.STATUS_REVIEW)
        self.assertEqual(transition["status"], "Review")

    def test_converted_to_draft_moves_status_to_working(self):
        transition = pr_triage.decide_transition("converted_to_draft")

        self.assertEqual(transition["status"], pr_triage.STATUS_WORKING)
        self.assertEqual(transition["status"], "Working")

    def test_opened_is_not_a_transition(self):
        self.assertIsNone(pr_triage.decide_transition("opened"))

    def test_reopened_draft_pr_moves_status_to_working(self):
        transition = pr_triage.decide_transition("reopened", is_draft=True)

        self.assertEqual(transition["status"], "Working")

    def test_reopened_ready_pr_moves_status_to_review(self):
        transition = pr_triage.decide_transition("reopened", is_draft=False)

        self.assertEqual(transition["status"], "Review")

    def test_transition_rule_is_decoupled_from_the_unknown_author_rule(self):
        with mock.patch.object(pr_triage, "UNKNOWN_AUTHOR_STATUS", "SomethingElse"):
            transition = pr_triage.decide_transition("ready_for_review")

        self.assertEqual(transition["status"], "Review")

    def test_transition_rule_is_decoupled_from_the_known_author_rule(self):
        with mock.patch.object(pr_triage, "KNOWN_AUTHOR_STATUS", "SomethingElse"):
            transition = pr_triage.decide_transition("converted_to_draft")

        self.assertEqual(transition["status"], "Working")


class CheckAssignmentSucceededTest(unittest.TestCase):
    def test_raises_when_author_login_is_absent_from_the_assignees_response(self):
        assign_result = {"assignees": []}

        with self.assertRaises(pr_triage.AssignmentDroppedError) as context:
            pr_triage.check_assignment_succeeded("someuser", assign_result)

        self.assertIn("someuser", str(context.exception))
        self.assertIn("pr-triage-teams.yml", str(context.exception))

    def test_does_not_raise_when_author_login_is_present(self):
        assign_result = {"assignees": [{"login": "someuser"}]}

        pr_triage.check_assignment_succeeded("someuser", assign_result)


class StripEmojiPrefixTest(unittest.TestCase):
    def test_strips_leading_emoji_and_whitespace(self):
        self.assertEqual(pr_triage.strip_emoji_prefix("\U0001f6e0️ Working"), "Working")

    def test_leaves_plain_name_unchanged(self):
        self.assertEqual(pr_triage.strip_emoji_prefix("Icebox"), "Icebox")


class FindOptionIdByNameTest(unittest.TestCase):
    def test_matches_status_option_ignoring_emoji_prefix(self):
        options = [
            {"name": "\U0001f6e0️ Working", "id": "47fc9ee4"},
            {"name": "\U0001f50d Review", "id": "879449e7"},
        ]

        option_id = pr_triage.find_option_id_by_name(options, "Working", strip_emoji=True)

        self.assertEqual(option_id, "47fc9ee4")

    def test_matches_team_option_by_exact_name(self):
        options = [{"name": "Dev", "id": "f2a5529c"}, {"name": "Delivery", "id": "34b176a9"}]

        option_id = pr_triage.find_option_id_by_name(options, "Delivery", strip_emoji=False)

        self.assertEqual(option_id, "34b176a9")

    def test_raises_with_full_option_list_when_name_does_not_resolve(self):
        options = [{"name": "Dev", "id": "f2a5529c"}]

        with self.assertRaises(ValueError) as context:
            pr_triage.find_option_id_by_name(options, "Nonexistent", strip_emoji=False)

        self.assertIn("Dev", str(context.exception))
        self.assertIn("Nonexistent", str(context.exception))


class ParseTeamsMappingTest(unittest.TestCase):
    def test_parses_flat_login_to_team_mapping(self):
        text = (
            "# header comment\n"
            "teams:\n"
            "  mswertz: Dev\n"
            "  hslh: Delivery\n"
        )

        mapping = pr_triage.parse_teams_mapping(text)

        self.assertEqual(mapping, {"mswertz": "Dev", "hslh": "Delivery"})

    def test_ignores_blank_lines_and_trailing_comments(self):
        text = (
            "teams:\n"
            "\n"
            "  mswertz: Dev\n"
        )

        mapping = pr_triage.parse_teams_mapping(text)

        self.assertEqual(mapping, {"mswertz": "Dev"})


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
    def test_passes_when_working_is_a_live_status_option(self):
        status_options = [{"id": "47fc9ee4", "name": "\U0001f6e0️ Working"}]

        error = validate_pr_triage_teams.find_missing_status_option(status_options, "Working")

        self.assertIsNone(error)

    def test_flags_when_working_is_not_a_live_status_option(self):
        status_options = [{"id": "98236657", "name": "✅ Done"}]

        error = validate_pr_triage_teams.find_missing_status_option(status_options, "Working")

        self.assertIsNotNone(error)

    def test_passes_when_review_is_a_live_status_option(self):
        status_options = [{"id": "879449e7", "name": "\U0001f50d Review"}]

        error = validate_pr_triage_teams.find_missing_status_option(status_options, "Review")

        self.assertIsNone(error)

    def test_flags_when_review_is_not_a_live_status_option(self):
        status_options = [{"id": "98236657", "name": "✅ Done"}]

        error = validate_pr_triage_teams.find_missing_status_option(status_options, "Review")

        self.assertIsNotNone(error)


class ValidateTeamOptionTest(unittest.TestCase):
    def test_passes_when_dev_is_a_live_team_option(self):
        team_options = [{"id": "f2a5529c", "name": "Dev"}]

        error = validate_pr_triage_teams.find_missing_team_option(team_options, "Dev")

        self.assertIsNone(error)

    def test_flags_when_dev_is_not_a_live_team_option(self):
        team_options = [{"id": "34b176a9", "name": "Delivery"}]

        error = validate_pr_triage_teams.find_missing_team_option(team_options, "Dev")

        self.assertIsNotNone(error)


class MainWritesStepSummaryOnFailureTest(unittest.TestCase):
    def test_assign_failure_does_not_prevent_draft_or_board_writes_and_exits_non_zero(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if "convertPullRequestToDraft" in query:
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

        with tempfile.TemporaryDirectory() as tmp_dir:
            event_path = os.path.join(tmp_dir, "event.json")
            with open(event_path, "w", encoding="utf-8") as handle:
                json.dump(event, handle)
            summary_path = os.path.join(tmp_dir, "summary.md")
            open(summary_path, "w", encoding="utf-8").close()

            env = {
                "GITHUB_EVENT_PATH": event_path,
                "GITHUB_TOKEN": "fake-github-token",
                "PROJECT_BOARD_TOKEN": "fake-board-token",
                "GITHUB_STEP_SUMMARY": summary_path,
            }

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "assign_author", side_effect=pr_triage.GraphqlError("boom")
            ), mock.patch.object(pr_triage, "http_request", side_effect=fake_http_request):
                with self.assertRaises(SystemExit) as context:
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertNotEqual(context.exception.code, 0)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self.assertTrue(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertTrue(any("addProjectV2ItemById" in query_of(call) for call in calls))
        self.assertTrue(any("updateProjectV2ItemFieldValue" in query_of(call) for call in calls))

        self.assertIn("### PR triage", summary_text)
        self.assertIn("boom", summary_text)
        self.assertIn("Board item id", summary_text)
        self.assertIn("Status option id written", summary_text)
        self.assertIn("Team option id written", summary_text)


class MainWiringTest(unittest.TestCase):
    def test_known_author_writes_go_to_the_right_endpoint_field_and_token(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                return {"assignees": [{"login": "mswertz"}]}
            if "convertPullRequestToDraft" in query:
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        assign_call = next(call for call in calls if call["url"].endswith("/assignees"))
        self.assertEqual(assign_call["token"], "github-token")

        draft_call = next(call for call in calls if "convertPullRequestToDraft" in query_of(call))
        self.assertEqual(draft_call["token"], "board-token")

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 2)

        status_write = field_writes[0]
        self.assertEqual(status_write["body"]["variables"]["fieldId"], pr_triage.STATUS_FIELD_ID)
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_OPT")
        self.assertEqual(status_write["token"], "board-token")

        team_write = field_writes[1]
        self.assertEqual(team_write["body"]["variables"]["fieldId"], pr_triage.TEAM_FIELD_ID)
        self.assertEqual(team_write["body"]["variables"]["optionId"], "TEAM_OPT")
        self.assertEqual(team_write["token"], "board-token")


class MainRaisesOnDroppedAssignmentTest(unittest.TestCase):
    def test_dropped_assignment_does_not_prevent_draft_or_board_writes_and_exits_non_zero(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                return {"assignees": []}
            if "convertPullRequestToDraft" in query:
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                with self.assertRaises(SystemExit) as context:
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertNotEqual(context.exception.code, 0)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self.assertTrue(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertTrue(any("addProjectV2ItemById" in query_of(call) for call in calls))
        self.assertTrue(any("updateProjectV2ItemFieldValue" in query_of(call) for call in calls))

        self.assertIn("### PR triage", summary_text)
        self.assertIn("mswertz", summary_text)
        self.assertIn("Board item id", summary_text)


class MainDraftFailureStillBoardsAndExitsNonZeroTest(unittest.TestCase):
    def test_forbidden_draft_conversion_does_not_prevent_assignment_or_board_writes(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                return {"assignees": [{"login": "mswertz"}]}
            if "convertPullRequestToDraft" in query:
                raise pr_triage.GraphqlError(
                    "GraphQL request returned errors: [{'type': 'FORBIDDEN', "
                    "'message': 'Resource not accessible by integration'}]"
                )
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                with self.assertRaises(SystemExit) as context:
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertNotEqual(context.exception.code, 0)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        assign_call = next(call for call in calls if call["url"].endswith("/assignees"))
        self.assertEqual(assign_call["token"], "github-token")

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 2)

        self.assertIn("### PR triage", summary_text)
        self.assertIn("| Assignee set | True |", summary_text)
        self.assertIn("FORBIDDEN", summary_text)
        self.assertIn("Board item id", summary_text)
        self.assertIn("Status option id written", summary_text)
        self.assertIn("Team option id written", summary_text)


class MappingFilePathTest(unittest.TestCase):
    def test_is_absolute_even_when_file_argument_is_relative(self):
        result = pr_triage.mapping_file_path("some/relative/.github/scripts/pr_triage.py")

        self.assertTrue(os.path.isabs(result))

    def test_resolves_next_to_the_given_file_not_against_cwd(self):
        result = pr_triage.mapping_file_path("/opt/checkout/.github/scripts/pr_triage.py")

        self.assertEqual(result, "/opt/checkout/.github/pr-triage-teams.yml")


class MainWiringReopenedTest(unittest.TestCase):
    def _run_reopened(self, is_draft, item_exists):
        import tempfile

        event = {
            "action": "reopened",
            "pull_request": {
                "user": {"login": "mswertz"},
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
                raise AssertionError("reopened must never touch the assignee")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("reopened must never call convertPullRequestToDraft")
            if "ReadyForReview" in query:
                raise AssertionError("reopened must never call a ready-conversion mutation")

            if "projectItems" in query:
                if item_exists:
                    nodes = [
                        {
                            "id": "EXISTING_ITEM",
                            "project": {"id": pr_triage.BOARD_PROJECT_ID},
                            "fieldValueByName": {"name": "Working"},
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
                if variables.get("fieldId") == pr_triage.TEAM_FIELD_ID:
                    raise AssertionError("reopened must never write the Team field")
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM"}}}}

            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                raise AssertionError("reopened must never fetch Team options")

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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

        return calls

    def test_reopened_draft_pr_moves_status_to_working(self):
        calls = self._run_reopened(is_draft=True, item_exists=True)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_reopened_ready_pr_moves_status_to_review(self):
        calls = self._run_reopened(is_draft=False, item_exists=True)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "879449e7")

    def test_reopened_with_no_existing_item_is_added_not_skipped(self):
        calls = self._run_reopened(is_draft=False, item_exists=False)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        add_item_calls = [call for call in calls if "addProjectV2ItemById" in query_of(call)]
        self.assertEqual(len(add_item_calls), 1)


class MainWiringOpenedStillRunsFullOpenPathTest(unittest.TestCase):
    def test_opened_action_still_assigns_drafts_and_writes_team(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                return {"assignees": [{"login": "mswertz"}]}
            if "convertPullRequestToDraft" in query:
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        assign_call = next(call for call in calls if call["url"].endswith("/assignees"))
        self.assertEqual(assign_call["token"], "github-token")

        draft_call = next(call for call in calls if "convertPullRequestToDraft" in query_of(call))
        self.assertEqual(draft_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 2)
        self.assertEqual(field_writes[1]["body"]["variables"]["fieldId"], pr_triage.TEAM_FIELD_ID)


class MainWiringSynchronizeTest(unittest.TestCase):
    def _run_synchronize(self, is_draft, item_exists):
        import tempfile

        event = {
            "action": "synchronize",
            "pull_request": {
                "user": {"login": "mswertz"},
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
                raise AssertionError("synchronize must never touch the assignee")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("synchronize must never touch the draft state")
            if "ReadyForReview" in query:
                raise AssertionError("synchronize must never touch the draft state")

            if "projectItems" in query:
                if item_exists:
                    nodes = [
                        {
                            "id": "EXISTING_ITEM",
                            "project": {"id": pr_triage.BOARD_PROJECT_ID},
                            "fieldValueByName": {"name": "Working"},
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
                if variables.get("fieldId") == pr_triage.TEAM_FIELD_ID:
                    raise AssertionError("synchronize must never write the Team field")
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM"}}}}

            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                raise AssertionError("synchronize must never fetch Team options")

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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        return calls, summary_text

    def _assert_no_forbidden_calls(self, calls):
        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))
        self.assertFalse(any(
            "updateProjectV2ItemFieldValue" in query_of(call)
            and call["body"]["variables"].get("fieldId") == pr_triage.TEAM_FIELD_ID
            for call in calls
        ))

    def test_item_already_exists_does_nothing_at_all(self):
        calls, summary_text = self._run_synchronize(is_draft=False, item_exists=True)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self._assert_no_forbidden_calls(calls)
        self.assertFalse(any("addProjectV2ItemById" in query_of(call) for call in calls))
        self.assertFalse(any("updateProjectV2ItemFieldValue" in query_of(call) for call in calls))

        self.assertIn("### PR triage", summary_text)
        self.assertIn("no action", summary_text.lower())

    def test_item_missing_and_draft_adds_and_boards_working(self):
        calls, summary_text = self._run_synchronize(is_draft=True, item_exists=False)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self._assert_no_forbidden_calls(calls)

        add_item_calls = [call for call in calls if "addProjectV2ItemById" in query_of(call)]
        self.assertEqual(len(add_item_calls), 1)
        self.assertEqual(add_item_calls[0]["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage.STATUS_FIELD_ID)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")
        self.assertEqual(field_writes[0]["body"]["variables"]["itemId"], "NEW_ITEM")
        self.assertEqual(field_writes[0]["token"], "board-token")

        self.assertIn("### PR triage", summary_text)

    def test_item_missing_and_ready_adds_and_boards_review(self):
        calls, summary_text = self._run_synchronize(is_draft=False, item_exists=False)

        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self._assert_no_forbidden_calls(calls)

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], "879449e7")


class MainWiringUnknownAuthorTest(unittest.TestCase):
    def _run_main_for(self, is_draft):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                raise AssertionError("unknown author must never be assigned")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("unknown author's draft state must never be touched")
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_REVIEW_OPT", "name": "Review"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_DEV_OPT", "name": "Dev"}]}}}
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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        return calls, summary_text

    def _assert_unknown_author_boarded_correctly(self, calls, summary_text):
        def query_of(call):
            return (call["body"] or {}).get("query", "")

        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))

        add_item_call = next(call for call in calls if "addProjectV2ItemById" in query_of(call))
        self.assertEqual(add_item_call["token"], "board-token")

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 2)

        status_write = field_writes[0]
        self.assertEqual(status_write["body"]["variables"]["fieldId"], pr_triage.STATUS_FIELD_ID)
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_REVIEW_OPT")
        self.assertEqual(status_write["token"], "board-token")

        team_write = field_writes[1]
        self.assertEqual(team_write["body"]["variables"]["fieldId"], pr_triage.TEAM_FIELD_ID)
        self.assertEqual(team_write["body"]["variables"]["optionId"], "TEAM_DEV_OPT")
        self.assertEqual(team_write["token"], "board-token")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("| Assignee set | skipped, unknown author is never assigned |", summary_text)
        self.assertIn(
            "| Converted to draft | skipped, draft state left untouched for unknown author |", summary_text
        )
        self.assertIn("absent from .github/pr-triage-teams.yml", summary_text)

    def test_unknown_author_ready_for_review_is_boarded_without_assign_or_draft(self):
        calls, summary_text = self._run_main_for(is_draft=False)
        self._assert_unknown_author_boarded_correctly(calls, summary_text)

    def test_unknown_author_already_draft_is_boarded_without_assign_or_draft(self):
        calls, summary_text = self._run_main_for(is_draft=True)
        self._assert_unknown_author_boarded_correctly(calls, summary_text)


class MainResolvesOptionsBeforeAddingBoardItemTest(unittest.TestCase):
    def test_status_and_team_options_are_resolved_before_the_item_is_added(self):
        import tempfile

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

        call_kinds = []

        def fake_http_request(url, token, method="GET", body=None):
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if url.endswith("/assignees"):
                call_kinds.append("assign")
                return {"assignees": [{"login": "mswertz"}]}
            if "convertPullRequestToDraft" in query:
                call_kinds.append("draft")
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                call_kinds.append("add_item")
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                call_kinds.append("set_field")
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    call_kinds.append("fetch_status_options")
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    call_kinds.append("fetch_team_options")
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

        add_item_index = call_kinds.index("add_item")
        self.assertLess(call_kinds.index("fetch_status_options"), add_item_index)
        self.assertLess(call_kinds.index("fetch_team_options"), add_item_index)

    def test_a_resolution_failure_never_adds_a_board_item(self):
        import tempfile

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

        def fake_http_request(url, token, method="GET", body=None):
            query = (body or {}).get("query", "")
            calls.append(query)
            if url.endswith("/assignees"):
                return {"assignees": [{"login": "mswertz"}]}
            if "convertPullRequestToDraft" in query:
                return {"data": {"convertPullRequestToDraft": {"pullRequest": {"id": "PR_node", "isDraft": True}}}}
            if "addProjectV2ItemById" in query:
                raise AssertionError("must not add a board item when option resolution fails")
            if "options { id name }" in query:
                return {"data": {"node": {"options": [{"id": "SOME_OPT", "name": "NoMatchingOptionHere"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                with self.assertRaises(SystemExit) as context:
                    pr_triage.main()

        self.assertNotEqual(context.exception.code, 0)
        self.assertFalse(any("addProjectV2ItemById" in query for query in calls))


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
LIVE_STATUS_OPTION_ID_BY_NAME = {"Working": "47fc9ee4", "Review": "879449e7"}


class MainWiringTransitionTest(unittest.TestCase):
    def _run_transition(self, action, author_login, item_exists):
        import tempfile

        event = {
            "action": action,
            "pull_request": {
                "user": {"login": author_login},
                "head": {"ref": "feature/x"},
                "number": 1,
                "node_id": "PR_node",
                "draft": action == "converted_to_draft",
            },
            "repository": {"full_name": "molgenis/molgenis-emx2"},
        }

        calls = []

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})

            if url.endswith("/assignees"):
                raise AssertionError("a transition must never touch the assignee")
            if "convertPullRequestToDraft" in query:
                raise AssertionError("a transition must never call convertPullRequestToDraft")
            if "ReadyForReview" in query:
                raise AssertionError("a transition must never call a ready-conversion mutation")

            if "projectItems" in query:
                if item_exists:
                    nodes = [
                        {
                            "id": "EXISTING_ITEM",
                            "project": {"id": pr_triage.BOARD_PROJECT_ID},
                            "fieldValueByName": {"name": "Working"},
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
                if variables.get("fieldId") == pr_triage.TEAM_FIELD_ID:
                    raise AssertionError("a transition must never write the Team field")
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM"}}}}

            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                raise AssertionError("a transition must never fetch Team options")

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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        return calls, summary_text

    def _assert_transition_wiring(self, calls, summary_text, action, item_exists):
        def query_of(call):
            return (call["body"] or {}).get("query", "")

        expected_status_name = "Review" if action == "ready_for_review" else "Working"
        expected_option_id = LIVE_STATUS_OPTION_ID_BY_NAME[expected_status_name]
        expected_item_id = "EXISTING_ITEM" if item_exists else "NEW_ITEM"

        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertFalse(any("convertPullRequestToDraft" in query_of(call) for call in calls))

        add_item_calls = [call for call in calls if "addProjectV2ItemById" in query_of(call)]
        if item_exists:
            self.assertEqual(len(add_item_calls), 0)
        else:
            self.assertEqual(len(add_item_calls), 1)
            self.assertEqual(add_item_calls[0]["token"], "board-token")

        project_items_calls = [call for call in calls if "projectItems" in query_of(call)]
        self.assertEqual(len(project_items_calls), 1)
        self.assertIn("first: 100", query_of(project_items_calls[0]))

        field_writes = [call for call in calls if "updateProjectV2ItemFieldValue" in query_of(call)]
        self.assertEqual(len(field_writes), 1)
        self.assertEqual(field_writes[0]["body"]["variables"]["fieldId"], pr_triage.STATUS_FIELD_ID)
        self.assertEqual(field_writes[0]["body"]["variables"]["itemId"], expected_item_id)
        self.assertEqual(field_writes[0]["body"]["variables"]["optionId"], expected_option_id)
        self.assertEqual(field_writes[0]["token"], "board-token")

        self.assertIn("### PR triage", summary_text)
        self.assertIn("| Team | not touched, deliberate |", summary_text)
        self.assertIn(
            "| Assignee | not touched, deliberate, including when empty |", summary_text
        )

    def test_ready_for_review_known_author_existing_item(self):
        calls, summary_text = self._run_transition("ready_for_review", "mswertz", item_exists=True)
        self._assert_transition_wiring(calls, summary_text, "ready_for_review", item_exists=True)

    def test_ready_for_review_unknown_author_no_existing_item(self):
        calls, summary_text = self._run_transition("ready_for_review", "some-outside-contributor", item_exists=False)
        self._assert_transition_wiring(calls, summary_text, "ready_for_review", item_exists=False)

    def test_converted_to_draft_known_author_existing_item(self):
        calls, summary_text = self._run_transition("converted_to_draft", "mswertz", item_exists=True)
        self._assert_transition_wiring(calls, summary_text, "converted_to_draft", item_exists=True)

    def test_converted_to_draft_unknown_author_no_existing_item(self):
        calls, summary_text = self._run_transition(
            "converted_to_draft", "some-outside-contributor", item_exists=False
        )
        self._assert_transition_wiring(calls, summary_text, "converted_to_draft", item_exists=False)


class TransitionWritesStepSummaryOnFailureTest(unittest.TestCase):
    def test_step_summary_is_written_and_error_reraised_when_a_transition_write_fails(self):
        import tempfile

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
                pr_triage, "fetch_project_field_options", side_effect=pr_triage.GraphqlError("board boom")
            ):
                with self.assertRaises(pr_triage.GraphqlError):
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("### PR triage", summary_text)
        self.assertIn("Failure", summary_text)
        self.assertIn("board boom", summary_text)


class TransitionResolvesStatusBeforeAddingBoardItemTest(unittest.TestCase):
    def test_status_option_is_resolved_before_a_missing_item_is_added(self):
        import tempfile

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
                call_kinds.append("fetch_status_options")
                return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                pr_triage.main()

        self.assertLess(call_kinds.index("fetch_status_options"), call_kinds.index("add_item"))

    def test_a_status_resolution_failure_never_adds_a_board_item(self):
        import tempfile

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
                pr_triage, "http_request", side_effect=fake_http_request
            ):
                with self.assertRaises(ValueError):
                    pr_triage.main()


class MainIgnoresUnrecognizedActionTest(unittest.TestCase):
    def test_an_unrecognized_action_makes_no_writes_and_reads_no_mapping_file(self):
        import tempfile

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
                pr_triage, "http_request", side_effect=fake_http_request
            ), mock.patch.object(
                pr_triage, "load_teams_mapping", side_effect=AssertionError("must not read the mapping file")
            ):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("labeled", summary_text)
        self.assertIn("no action", summary_text.lower())


class MainReadsAssignDecisionTest(unittest.TestCase):
    def test_main_does_not_assign_when_decision_says_not_to(self):
        import tempfile

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

        forced_decision = {
            "known": True,
            "assign": False,
            "force_draft": False,
            "status": "Working",
            "team": "Dev",
        }

        calls = []

        def fake_http_request(url, token, method="GET", body=None):
            calls.append({"url": url, "token": token, "body": body})
            query = (body or {}).get("query", "")
            variables = (body or {}).get("variables", {})
            if "addProjectV2ItemById" in query:
                return {"data": {"addProjectV2ItemById": {"item": {"id": "ITEM_1"}}}}
            if "updateProjectV2ItemFieldValue" in query:
                return {"data": {"updateProjectV2ItemFieldValue": {"projectV2Item": {"id": "ITEM_1"}}}}
            if "options { id name }" in query:
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "Working"}]}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
                    return {"data": {"node": {"options": [{"id": "TEAM_OPT", "name": "Dev"}]}}}
            raise AssertionError(f"unexpected call: {url} {query}")

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

            with mock.patch.dict(os.environ, env, clear=False), mock.patch.object(
                pr_triage, "http_request", side_effect=fake_http_request
            ), mock.patch.object(pr_triage, "decide", return_value=forced_decision):
                pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertFalse(any(call["url"].endswith("/assignees") for call in calls))
        self.assertIn("| Assignee set | skipped, not required by decision |", summary_text)


class FindBoardItemForPrTest(unittest.TestCase):
    def test_returns_the_matching_project_item_and_its_current_status(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "OTHER_ITEM",
                                "project": {"id": "PVT_someOtherBoard"},
                                "fieldValueByName": {"name": "Done"},
                            },
                            {
                                "id": "EXISTING_ITEM",
                                "project": {"id": pr_triage.BOARD_PROJECT_ID},
                                "fieldValueByName": {"name": "Working"},
                            },
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertEqual(item["old_status"], "Working")

    def test_returns_none_when_pr_has_no_item_on_this_board(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "OTHER_ITEM",
                                "project": {"id": "PVT_someOtherBoard"},
                                "fieldValueByName": {"name": "Done"},
                            }
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(item)

    def test_returns_none_when_pr_has_no_items_at_all(self):
        response = {"data": {"node": {"projectItems": {"nodes": []}}}}
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(item)

    def test_old_status_is_none_when_status_field_is_unset(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "EXISTING_ITEM",
                                "project": {"id": pr_triage.BOARD_PROJECT_ID},
                                "fieldValueByName": None,
                            }
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertIsNone(item["old_status"])


class GraphqlRequestErrorHandlingTest(unittest.TestCase):
    def test_raises_when_response_body_carries_errors_despite_http_200(self):
        errors_payload = [{"message": "Resource not accessible - requires one of the following scopes: ['project']"}]
        with mock.patch.object(pr_triage, "http_request", return_value={"data": None, "errors": errors_payload}):
            with self.assertRaises(pr_triage.GraphqlError) as context:
                pr_triage.graphql_request("query {}", {}, "token")

        self.assertIn("requires one of the following scopes", str(context.exception))

    def test_passes_through_a_clean_response(self):
        with mock.patch.object(pr_triage, "http_request", return_value={"data": {"ok": True}}):
            result = pr_triage.graphql_request("query {}", {}, "token")

        self.assertEqual(result, {"data": {"ok": True}})


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
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            scripts_dir, _ = self._write_clean_triage_source(tmp_dir)
            with open(os.path.join(scripts_dir, "pr_triage.py"), "w", encoding="utf-8") as handle:
                handle.write("if pull_request.author_association == 'OWNER':\n    pass\n")

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [("pr_triage.py", ["author_association"])])

    def test_flags_a_banned_term_planted_in_the_workflow_file_itself(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            _, workflows_dir = self._write_clean_triage_source(tmp_dir)
            with open(os.path.join(workflows_dir, "pr-triage.yml"), "w", encoding="utf-8") as handle:
                handle.write("if: github.event.pull_request.author_association == 'OWNER'\n")

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [("pr-triage.yml", ["author_association"])])

    def test_passes_clean_triage_source(self):
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            self._write_clean_triage_source(tmp_dir)

            findings = validate_pr_triage_teams.find_banned_terms_across_triage_source(tmp_dir)

        self.assertEqual(findings, [])


class ValidateMainExitsNonZeroOnDuplicateLoginTest(unittest.TestCase):
    def test_main_exits_non_zero_when_a_login_is_listed_twice(self):
        import tempfile

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
            fake_status_options = [{"id": "s1", "name": "Working"}, {"id": "s2", "name": "Review"}]
            fake_team_options = [{"id": "t1", "name": "Dev"}, {"id": "t2", "name": "Delivery"}]

            def fake_fetch(field_id, token):
                if field_id == pr_triage.TEAM_FIELD_ID:
                    return fake_team_options
                return fake_status_options

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage, "fetch_project_field_options", side_effect=fake_fetch
            ):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class ValidateMainChecksTransitionStatusConstantsTest(unittest.TestCase):
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
        import tempfile

        with tempfile.TemporaryDirectory() as tmp_dir:
            repo_root = self._write_clean_repo(tmp_dir)

            env = {"PROJECT_BOARD_TOKEN": "board-token"}
            fake_status_options = [{"id": "s1", "name": "Working"}, {"id": "s2", "name": "Review"}]
            fake_team_options = [{"id": "t1", "name": "Dev"}]

            def fake_fetch(field_id, token):
                if field_id == pr_triage.TEAM_FIELD_ID:
                    return fake_team_options
                return fake_status_options

            with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(
                pr_triage, "fetch_project_field_options", side_effect=fake_fetch
            ), mock.patch.object(pr_triage, "STATUS_REVIEW", "DivergedFromUnknownAuthorStatus"):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class ValidateMainExitsNonZeroOnBannedTermTest(unittest.TestCase):
    def test_main_exits_non_zero_when_a_banned_term_is_planted(self):
        import tempfile

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
                pr_triage, "fetch_project_field_options", return_value=fake_options
            ):
                with self.assertRaises(SystemExit) as context:
                    validate_pr_triage_teams.main(repo_root=repo_root)

        self.assertNotEqual(context.exception.code, 0)


class WorkflowHasAPerPrConcurrencyGroupTest(unittest.TestCase):
    def test_concurrency_group_is_scoped_per_pr_and_does_not_cancel_in_progress_runs(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        self.assertIn("concurrency:", workflow_text)
        self.assertIn("github.event.pull_request.number", workflow_text)
        self.assertNotIn("cancel-in-progress: true", workflow_text)


class WorkflowGithubTokenPermissionsAreNarrowedTest(unittest.TestCase):
    def test_triage_job_no_longer_grants_pull_requests_write(self):
        repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        workflow_path = os.path.join(repo_root, ".github", "workflows", "pr-triage.yml")
        with open(workflow_path, encoding="utf-8") as handle:
            workflow_text = handle.read()

        self.assertIn("issues: write", workflow_text)
        self.assertNotIn("pull-requests: write", workflow_text)


if __name__ == "__main__":
    unittest.main()
