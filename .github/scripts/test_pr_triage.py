import datetime
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
        self.assertEqual(decision["team"], "Dev")

    def test_unknown_author_already_draft_is_still_not_touched(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="some-outside-contributor", is_draft=True, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["team"], "Dev")

    def test_bot_author_is_unknown_regardless_of_login_shape(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="dependabot[bot]", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["assign"])
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
        self.assertEqual(decision["team"], "Dev")

    def test_whitespace_only_team_value_is_treated_as_unknown(self):
        mapping = {"someuser": "   "}

        decision = pr_triage.decide(author_login="someuser", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])

    def test_login_matching_is_exact_case_and_can_only_ever_miss(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage.decide(author_login="Mswertz", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])


class DecideStatusTest(unittest.TestCase):
    def test_blank_status_fills_from_draft_state(self):
        self.assertEqual(pr_triage.decide_status(True, None), "Working")
        self.assertEqual(pr_triage.decide_status(False, None), "Review")

    def test_blank_string_status_counts_as_blank(self):
        self.assertEqual(pr_triage.decide_status(True, "   "), "Working")

    def test_working_flips_to_review_when_pr_is_ready(self):
        self.assertEqual(pr_triage.decide_status(False, "Working"), "Review")

    def test_review_flips_to_working_when_pr_is_draft(self):
        self.assertEqual(pr_triage.decide_status(True, "Review"), "Working")

    def test_working_stays_working_when_pr_is_draft(self):
        self.assertEqual(pr_triage.decide_status(True, "Working"), "Working")

    def test_review_stays_review_when_pr_is_ready(self):
        self.assertEqual(pr_triage.decide_status(False, "Review"), "Review")

    def test_non_managed_statuses_are_never_touched_regardless_of_draft_state(self):
        for status in ("Epic", "Blocked", "Backlog", "Done", "Inbox", "Icebox"):
            with self.subTest(status=status):
                self.assertIsNone(pr_triage.decide_status(True, status))
                self.assertIsNone(pr_triage.decide_status(False, status))

    def test_all_eight_real_board_option_names_behave_correctly_for_both_draft_states(self):
        expected = {
            "\U0001f4cb Epic": (None, None),
            "⛔️ Blocked": (None, None),
            "\U0001f4da Backlog": (None, None),
            "\U0001f6e0️ Working": ("Working", "Review"),
            "\U0001f50d Review": ("Working", "Review"),
            "✅ Done": (None, None),
            "\U0001f4e5 Inbox": (None, None),
            "Icebox": (None, None),
        }

        for real_name, (expected_draft, expected_ready) in expected.items():
            with self.subTest(real_name=real_name):
                self.assertEqual(pr_triage.decide_status(True, real_name), expected_draft)
                self.assertEqual(pr_triage.decide_status(False, real_name), expected_ready)


class DecideBoardUpdateTest(unittest.TestCase):
    def test_ready_for_review_sets_status_review_outright(self):
        fields = pr_triage.decide_board_update(
            "ready_for_review", is_draft=False, current_status="🛠️ Working", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Review")

    def test_converted_to_draft_sets_status_working_outright(self):
        fields = pr_triage.decide_board_update(
            "converted_to_draft", is_draft=True, current_status="🔍 Review", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Working")

    def test_reopened_draft_sets_status_working_outright(self):
        fields = pr_triage.decide_board_update(
            "reopened", is_draft=True, current_status="🔍 Review", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Working")

    def test_reopened_ready_sets_status_review_outright(self):
        fields = pr_triage.decide_board_update(
            "reopened", is_draft=False, current_status="🛠️ Working", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Review")

    def test_transition_flips_a_managed_status_to_match_current_draft_state(self):
        fields = pr_triage.decide_board_update(
            "reopened", is_draft=True, current_status="🔍 Review", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Working")

    def test_transition_fills_team_when_empty(self):
        fields = pr_triage.decide_board_update(
            "ready_for_review", is_draft=False, current_status="🛠️ Working", current_team=None, mapped_team="Delivery"
        )

        self.assertEqual(fields["team"], "Delivery")

    def test_transition_never_overwrites_a_non_empty_team(self):
        fields = pr_triage.decide_board_update(
            "ready_for_review",
            is_draft=False,
            current_status="🛠️ Working",
            current_team="Analysis",
            mapped_team="Delivery",
        )

        self.assertIsNone(fields["team"])

    def test_synchronize_fills_status_only_when_empty_and_draft(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=True, current_status=None, current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Working")

    def test_synchronize_fills_status_only_when_empty_and_ready(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=False, current_status=None, current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Review")

    def test_synchronize_flips_a_managed_status_to_match_current_draft_state(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=True, current_status="🔍 Review", current_team="Dev", mapped_team="Dev"
        )

        self.assertEqual(fields["status"], "Working")

    def test_synchronize_leaves_a_non_managed_status_alone(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=True, current_status="📋 Epic", current_team="Dev", mapped_team="Dev"
        )

        self.assertIsNone(fields["status"])

    def test_transition_leaves_a_non_managed_status_alone(self):
        fields = pr_triage.decide_board_update(
            "ready_for_review", is_draft=False, current_status="⛔️ Blocked", current_team="Dev", mapped_team="Dev"
        )

        self.assertIsNone(fields["status"])

    def test_synchronize_fills_team_when_empty(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=False, current_status="🔍 Review", current_team=None, mapped_team="Delivery"
        )

        self.assertEqual(fields["team"], "Delivery")

    def test_synchronize_never_overwrites_a_non_empty_team(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=False, current_status="🔍 Review", current_team="Analysis", mapped_team="Delivery"
        )

        self.assertIsNone(fields["team"])

    def test_synchronize_leaves_non_managed_status_alone_while_still_filling_team(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=True, current_status="📋 Epic", current_team=None, mapped_team="Delivery"
        )

        self.assertIsNone(fields["status"])
        self.assertEqual(fields["team"], "Delivery")

    def test_blank_string_team_counts_as_empty(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=False, current_status="🔍 Review", current_team="   ", mapped_team="Delivery"
        )

        self.assertEqual(fields["team"], "Delivery")

    def test_opened_is_not_a_board_update_action(self):
        self.assertIsNone(
            pr_triage.decide_board_update(
                "opened", is_draft=False, current_status=None, current_team=None, mapped_team="Dev"
            )
        )

    def test_unrecognized_action_is_not_a_board_update_action(self):
        self.assertIsNone(
            pr_triage.decide_board_update(
                "labeled", is_draft=False, current_status=None, current_team=None, mapped_team="Dev"
            )
        )

    def test_transition_fills_sprint_when_empty(self):
        fields = pr_triage.decide_board_update(
            "ready_for_review",
            is_draft=False,
            current_status="🛠️ Working",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint=None,
            current_sprint_title="Sprint 260",
        )

        self.assertEqual(fields["sprint"], "Sprint 260")

    def test_transition_never_overwrites_a_non_empty_sprint(self):
        fields = pr_triage.decide_board_update(
            "converted_to_draft",
            is_draft=True,
            current_status="🔍 Review",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint="Sprint 259",
            current_sprint_title="Sprint 260",
        )

        self.assertIsNone(fields["sprint"])

    def test_reopened_fills_sprint_when_empty(self):
        fields = pr_triage.decide_board_update(
            "reopened",
            is_draft=False,
            current_status="🛠️ Working",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint=None,
            current_sprint_title="Sprint 260",
        )

        self.assertEqual(fields["sprint"], "Sprint 260")

    def test_synchronize_fills_sprint_when_empty(self):
        fields = pr_triage.decide_board_update(
            "synchronize",
            is_draft=False,
            current_status="🔍 Review",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint=None,
            current_sprint_title="Sprint 260",
        )

        self.assertEqual(fields["sprint"], "Sprint 260")

    def test_synchronize_never_overwrites_a_non_empty_sprint(self):
        fields = pr_triage.decide_board_update(
            "synchronize",
            is_draft=False,
            current_status="🔍 Review",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint="Sprint 259",
            current_sprint_title="Sprint 260",
        )

        self.assertIsNone(fields["sprint"])

    def test_blank_string_sprint_counts_as_empty(self):
        fields = pr_triage.decide_board_update(
            "synchronize",
            is_draft=False,
            current_status="🔍 Review",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint="   ",
            current_sprint_title="Sprint 260",
        )

        self.assertEqual(fields["sprint"], "Sprint 260")

    def test_empty_sprint_stays_unwritten_when_no_current_iteration_resolves(self):
        fields = pr_triage.decide_board_update(
            "synchronize",
            is_draft=False,
            current_status="🔍 Review",
            current_team="Dev",
            mapped_team="Dev",
            current_sprint=None,
            current_sprint_title=None,
        )

        self.assertIsNone(fields["sprint"])

    def test_no_field_is_written_by_default_when_sprint_args_are_omitted(self):
        fields = pr_triage.decide_board_update(
            "synchronize", is_draft=False, current_status="🔍 Review", current_team="Dev", mapped_team="Dev"
        )

        self.assertIsNone(fields["sprint"])


class FindCurrentIterationTest(unittest.TestCase):
    def test_returns_the_iteration_containing_today(self):
        import datetime

        iterations = [
            {"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21},
            {"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21},
        ]

        iteration = pr_triage.find_current_iteration(iterations, datetime.date(2026, 8, 8))

        self.assertEqual(iteration["id"], "bd551114")
        self.assertEqual(iteration["title"], "Sprint 260")

    def test_start_date_itself_is_inside_the_iteration(self):
        import datetime

        iterations = [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}]

        iteration = pr_triage.find_current_iteration(iterations, datetime.date(2026, 8, 3))

        self.assertEqual(iteration["id"], "bd551114")

    def test_the_day_after_the_iteration_ends_is_not_inside_it(self):
        import datetime

        iterations = [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}]

        end_date = datetime.date(2026, 8, 3) + datetime.timedelta(days=21)
        iteration = pr_triage.find_current_iteration(iterations, end_date)

        self.assertIsNone(iteration)

    def test_returns_none_when_no_iteration_contains_the_date(self):
        import datetime

        iterations = [{"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21}]

        iteration = pr_triage.find_current_iteration(iterations, datetime.date(2026, 9, 1))

        self.assertIsNone(iteration)

    def test_returns_none_for_an_empty_iteration_list(self):
        import datetime

        self.assertIsNone(pr_triage.find_current_iteration([], datetime.date(2026, 8, 8)))

    def test_does_not_assume_the_first_entry_is_current(self):
        import datetime

        iterations = [
            {"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21},
            {"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21},
        ]

        iteration = pr_triage.find_current_iteration(iterations, datetime.date(2026, 7, 20))

        self.assertEqual(iteration["id"], "bd551113")


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
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "🛠️ Working"}]}}}
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
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "🛠️ Working"}]}}}
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
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "🛠️ Working"}]}}}
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
                    return {
                        "data": {
                            "node": {
                                "options": [
                                    {"id": "STATUS_WORKING_OPT", "name": "🛠️ Working"},
                                    {"id": "STATUS_REVIEW_OPT", "name": "🔍 Review"},
                                ]
                            }
                        }
                    }
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

        status_write = next(
            call for call in field_writes if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
        )
        self.assertEqual(status_write["body"]["variables"]["optionId"], "STATUS_REVIEW_OPT")

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
        import tempfile

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
                            "project": {"id": pr_triage.BOARD_PROJECT_ID},
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
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
                    return {"data": {"node": {"options": LIVE_STATUS_OPTIONS}}}
                if variables["fieldId"] == pr_triage.TEAM_FIELD_ID:
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
                pr_triage, "http_request", side_effect=fake_http_request
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

    # --- transitions: Status is always overwritten ---

    def test_ready_for_review_overwrites_status_even_when_already_set(self):
        calls, _ = self._run_board_update(
            "ready_for_review", is_draft=False, item_exists=True, current_status="🛠️ Working", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_reopened_draft_overwrites_status_to_working(self):
        calls, _ = self._run_board_update(
            "reopened", is_draft=True, item_exists=True, current_status="🔍 Review", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes[0]["body"]["variables"]["optionId"], "47fc9ee4")

    def test_reopened_ready_overwrites_status_to_review(self):
        calls, _ = self._run_board_update(
            "reopened", is_draft=False, item_exists=True, current_status="🛠️ Working", current_team="Dev"
        )

        status_writes = [
            call
            for call in self._field_writes(calls)
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
        ]
        self.assertEqual(status_writes, [])

    # --- transitions: Team and Sprint fill only, never overwrite ---

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
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage.TEAM_FIELD_ID
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
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage.TEAM_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.SPRINT_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.SPRINT_FIELD_ID
        ]
        self.assertEqual(sprint_writes, [])

    # --- synchronize: fill-only for every field, never overwrite ---

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
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.STATUS_FIELD_ID
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
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage.TEAM_FIELD_ID
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
            call for call in self._field_writes(calls) if call["body"]["variables"]["fieldId"] == pr_triage.TEAM_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.SPRINT_FIELD_ID
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
            if call["body"]["variables"]["fieldId"] == pr_triage.SPRINT_FIELD_ID
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
            written_field_ids, {pr_triage.STATUS_FIELD_ID, pr_triage.TEAM_FIELD_ID, pr_triage.SPRINT_FIELD_ID}
        )
        for call in field_writes:
            self.assertEqual(call["body"]["variables"]["itemId"], "NEW_ITEM")
            self.assertEqual(call["token"], "board-token")


class BoardUpdateWritesStepSummaryOnFailureTest(unittest.TestCase):
    def test_step_summary_is_written_and_error_reraised_when_a_board_write_fails(self):
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
                pr_triage, "find_board_item_for_pr", side_effect=pr_triage.GraphqlError("board boom")
            ):
                with self.assertRaises(pr_triage.GraphqlError):
                    pr_triage.main()

            with open(summary_path, encoding="utf-8") as handle:
                summary_text = handle.read()

        self.assertIn("### PR triage", summary_text)
        self.assertIn("Failure", summary_text)
        self.assertIn("board boom", summary_text)


class BoardUpdateResolvesFieldsBeforeAddingBoardItemTest(unittest.TestCase):
    def test_fields_are_resolved_before_a_missing_item_is_added(self):
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
                call_kinds.append("fetch_options")
                if variables["fieldId"] == pr_triage.STATUS_FIELD_ID:
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
                pr_triage, "http_request", side_effect=fake_http_request
            ), mock.patch.object(pr_triage, "load_teams_mapping", return_value={"mswertz": "Dev"}), mock.patch.object(
                pr_triage, "current_date", return_value=datetime.date(2026, 8, 8)
            ):
                pr_triage.main()

        add_item_index = call_kinds.index("add_item")
        self.assertLess(call_kinds.index("fetch_options"), add_item_index)

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
                pr_triage, "http_request", side_effect=fake_http_request
            ), mock.patch.object(pr_triage, "load_teams_mapping", return_value={"mswertz": "Dev"}), mock.patch.object(
                pr_triage, "current_date", return_value=datetime.date(2026, 8, 8)
            ):
                with self.assertRaises(ValueError):
                    pr_triage.main()


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
                    return {
                        "data": {
                            "node": {
                                "options": [
                                    {"id": "STATUS_WORKING_OPT", "name": "🛠️ Working"},
                                    {"id": "STATUS_REVIEW_OPT", "name": "🔍 Review"},
                                ]
                            }
                        }
                    }
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

    def _assert_unknown_author_boarded_correctly(self, calls, summary_text, expected_status_option_id):
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
        self.assertEqual(status_write["body"]["variables"]["optionId"], expected_status_option_id)
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

    def test_unknown_author_ready_for_review_is_boarded_review(self):
        calls, summary_text = self._run_main_for(is_draft=False)
        self._assert_unknown_author_boarded_correctly(calls, summary_text, "STATUS_REVIEW_OPT")

    def test_unknown_author_already_draft_is_boarded_working(self):
        calls, summary_text = self._run_main_for(is_draft=True)
        self._assert_unknown_author_boarded_correctly(calls, summary_text, "STATUS_WORKING_OPT")


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
                    return {"data": {"node": {"options": [{"id": "STATUS_OPT", "name": "🛠️ Working"}]}}}
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
                    return {
                        "data": {
                            "node": {
                                "options": [
                                    {"id": "STATUS_WORKING_OPT", "name": "🛠️ Working"},
                                    {"id": "STATUS_REVIEW_OPT", "name": "🔍 Review"},
                                ]
                            }
                        }
                    }
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
                                "project": {"id": pr_triage.BOARD_PROJECT_ID},
                                "status": {"name": "🛠️ Working"},
                                "team": {"name": "Dev"},
                                "sprint": {"title": "Sprint 260"},
                            },
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertEqual(item["status"], "🛠️ Working")
        self.assertEqual(item["team"], "Dev")
        self.assertEqual(item["sprint"], "Sprint 260")

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
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(item)

    def test_returns_none_when_pr_has_no_items_at_all(self):
        response = {"data": {"node": {"projectItems": {"nodes": []}}}}
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertIsNone(item)

    def test_status_team_and_sprint_are_none_when_the_fields_are_unset(self):
        response = {
            "data": {
                "node": {
                    "projectItems": {
                        "nodes": [
                            {
                                "id": "EXISTING_ITEM",
                                "project": {"id": pr_triage.BOARD_PROJECT_ID},
                                "status": None,
                                "team": None,
                                "sprint": None,
                            }
                        ]
                    }
                }
            }
        }
        with mock.patch.object(pr_triage, "http_request", return_value=response):
            item = pr_triage.find_board_item_for_pr("PR_node", "board-token")

        self.assertEqual(item["id"], "EXISTING_ITEM")
        self.assertIsNone(item["status"])
        self.assertIsNone(item["team"])
        self.assertIsNone(item["sprint"])


class FetchProjectIterationsTest(unittest.TestCase):
    def test_returns_the_not_yet_completed_iterations(self):
        response = {
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
        with mock.patch.object(pr_triage, "http_request", return_value=response) as mock_request:
            iterations = pr_triage.fetch_project_iterations(pr_triage.SPRINT_FIELD_ID, "board-token")

        self.assertEqual(iterations, [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}])
        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertIn("iterations", called_body["query"])
        self.assertNotIn("completedIterations", called_body["query"])


class SetProjectFieldIterationTest(unittest.TestCase):
    def test_writes_the_iteration_id_not_a_single_select_option(self):
        with mock.patch.object(pr_triage, "http_request", return_value={"data": {}}) as mock_request:
            pr_triage.set_project_field_iteration("ITEM_1", pr_triage.SPRINT_FIELD_ID, "bd551114", "board-token")

        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertEqual(called_body["variables"]["iterationId"], "bd551114")
        self.assertIn("iterationId", called_body["query"])
        self.assertNotIn("singleSelectOptionId", called_body["query"])


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
            fake_status_options = [{"id": "s1", "name": "🛠️ Working"}, {"id": "s2", "name": "🔍 Review"}]
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
            fake_status_options = [{"id": "s1", "name": "🛠️ Working"}, {"id": "s2", "name": "🔍 Review"}]
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


if __name__ == "__main__":
    unittest.main()
