"""Tests for pr_triage_decide — the pure verdicts.

A test belongs here when the function it exercises needs no network, no clock and
no environment. This file imports pr_triage_decide and nothing else of ours; if a
test you are adding needs pr_triage or pr_triage_github, it belongs in their files.
"""

import datetime
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage_decide


class DecidePrTriageTest(unittest.TestCase):
    def test_known_author_is_assigned_drafted_and_boarded_working(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="mswertz", is_draft=False, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertTrue(decision["force_draft"])
        self.assertEqual(decision["team"], "Dev")

    def test_known_author_mapped_to_delivery_boards_under_delivery(self):
        mapping = {"hslh": "Delivery"}

        decision = pr_triage_decide.decide(author_login="hslh", is_draft=True, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertEqual(decision["team"], "Delivery")

    def test_unknown_author_is_boarded_review_dev_without_assign_or_draft(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="some-outside-contributor", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["team"], "Dev")

    def test_unknown_author_already_draft_is_still_not_touched(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="some-outside-contributor", is_draft=True, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["team"], "Dev")

    def test_bot_author_is_unknown_regardless_of_login_shape(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="dependabot[bot]", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertEqual(decision["team"], "Dev")

    def test_known_author_already_draft_is_not_re_converted(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="mswertz", is_draft=True, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertFalse(decision["force_draft"])

    def test_known_author_ready_for_review_is_converted_to_draft(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="mswertz", is_draft=False, mapping=mapping)

        self.assertTrue(decision["known"])
        self.assertTrue(decision["force_draft"])

    def test_empty_team_value_is_treated_as_unknown(self):
        mapping = {"someuser": ""}

        decision = pr_triage_decide.decide(author_login="someuser", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])
        self.assertFalse(decision["force_draft"])
        self.assertEqual(decision["team"], "Dev")

    def test_whitespace_only_team_value_is_treated_as_unknown(self):
        mapping = {"someuser": "   "}

        decision = pr_triage_decide.decide(author_login="someuser", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])

    def test_login_matching_is_exact_case_and_can_only_ever_miss(self):
        mapping = {"mswertz": "Dev"}

        decision = pr_triage_decide.decide(author_login="Mswertz", is_draft=False, mapping=mapping)

        self.assertFalse(decision["known"])


class DecideStatusTest(unittest.TestCase):
    def test_blank_status_fills_from_draft_state(self):
        self.assertEqual(pr_triage_decide.decide_status(True, None), "Working")
        self.assertEqual(pr_triage_decide.decide_status(False, None), "Review")

    def test_blank_string_status_counts_as_blank(self):
        self.assertEqual(pr_triage_decide.decide_status(True, "   "), "Working")

    def test_working_flips_to_review_when_pr_is_ready(self):
        self.assertEqual(pr_triage_decide.decide_status(False, "Working"), "Review")

    def test_review_flips_to_working_when_pr_is_draft(self):
        self.assertEqual(pr_triage_decide.decide_status(True, "Review"), "Working")

    def test_working_already_matching_draft_state_is_not_rewritten(self):
        self.assertIsNone(pr_triage_decide.decide_status(True, "Working"))

    def test_review_already_matching_ready_state_is_not_rewritten(self):
        self.assertIsNone(pr_triage_decide.decide_status(False, "Review"))

    def test_non_managed_statuses_are_never_touched_regardless_of_draft_state(self):
        for status in ("Epic", "Blocked", "Backlog", "Done", "Inbox", "Icebox"):
            with self.subTest(status=status):
                self.assertIsNone(pr_triage_decide.decide_status(True, status))
                self.assertIsNone(pr_triage_decide.decide_status(False, status))

    def test_all_eight_real_board_option_names_behave_correctly_for_both_draft_states(self):
        expected = {
            "\U0001f4cb Epic": (None, None),
            "⛔️ Blocked": (None, None),
            "\U0001f4da Backlog": (None, None),
            "\U0001f6e0️ Working": (None, "Review"),
            "\U0001f50d Review": ("Working", None),
            "✅ Done": (None, None),
            "\U0001f4e5 Inbox": (None, None),
            "Icebox": (None, None),
        }

        for real_name, (expected_draft, expected_ready) in expected.items():
            with self.subTest(real_name=real_name):
                self.assertEqual(pr_triage_decide.decide_status(True, real_name), expected_draft)
                self.assertEqual(pr_triage_decide.decide_status(False, real_name), expected_ready)


class DecideBoardUpdateTest(unittest.TestCase):
    """decide_board_update takes no `action` argument, so a subTest loop over
    BOARD_UPDATE_ACTIONS proved nothing beyond running the same assertion 5
    times. Every other case this class used to cover that way is provably
    redundant against MainWiringBoardUpdateTest and DecideStatusTest, per an
    owner-approved mutation-tested trim. This one survives: without it, a
    mutant that always overwrites a non-empty Sprint escapes every other test."""

    def test_sprint_never_overwrites_a_non_empty_sprint(self):
        for action in pr_triage_decide.BOARD_UPDATE_ACTIONS:
            with self.subTest(action=action):
                fields = pr_triage_decide.decide_board_update(
                    is_draft=True,
                    current_status="🔍 Review",
                    current_team="Dev",
                    mapped_team="Dev",
                    current_sprint="Sprint 259",
                    current_sprint_title="Sprint 260",
                )
                self.assertIsNone(fields["sprint"])


class KeywordClosingIssuesTest(unittest.TestCase):
    """Owner ruling, mid-ticket: only a KEYWORD-derived closing reference
    redirects aim 6 -- a sidebar-only link (GitHub's Development panel, no
    keyword in the body, e.g. this repo's `Addresses: <url>` habit) does not.
    keyword_closing_issues = closingIssuesReferences minus its
    userLinkedOnly=true counterpart, matched by node id. Verified against two
    real PRs of each shape, see notes/github-facts.md §8."""

    ISSUE_A = {"id": "ISSUE_A", "number": 1}
    ISSUE_B = {"id": "ISSUE_B", "number": 2}

    def test_a_keyword_only_link_counts(self):
        result = pr_triage_decide.keyword_closing_issues([self.ISSUE_A], [])

        self.assertEqual(result, [self.ISSUE_A])

    def test_a_sidebar_only_link_does_not_count(self):
        result = pr_triage_decide.keyword_closing_issues([self.ISSUE_A], [self.ISSUE_A])

        self.assertEqual(result, [])

    def test_both_sets_empty_means_no_redirection(self):
        result = pr_triage_decide.keyword_closing_issues([], [])

        self.assertEqual(result, [])

    def test_a_keyword_link_and_a_separate_sidebar_link_keeps_only_the_keyword_one(self):
        result = pr_triage_decide.keyword_closing_issues([self.ISSUE_A, self.ISSUE_B], [self.ISSUE_B])

        self.assertEqual(result, [self.ISSUE_A])

    def test_two_keyword_links_both_count(self):
        result = pr_triage_decide.keyword_closing_issues([self.ISSUE_A, self.ISSUE_B], [])

        self.assertEqual(result, [self.ISSUE_A, self.ISSUE_B])


class DecideRedirectToIssuesTest(unittest.TestCase):
    def test_true_when_the_pr_closes_one_issue(self):
        self.assertTrue(pr_triage_decide.decide_redirect_to_issues([{"id": "ISSUE_A", "number": 1}]))

    def test_true_when_the_pr_closes_two_issues(self):
        self.assertTrue(
            pr_triage_decide.decide_redirect_to_issues([{"id": "ISSUE_A", "number": 1}, {"id": "ISSUE_B", "number": 2}])
        )

    def test_false_when_the_pr_closes_no_issue(self):
        self.assertFalse(pr_triage_decide.decide_redirect_to_issues([]))


class DecideRemovePrCardTest(unittest.TestCase):
    """Renamed from decide_delete_pr_item -- owner ruling: "delete" reads as
    "delete the pull request", and it does not; this only removes a card
    from board 15."""

    def test_true_when_the_pr_closes_an_issue_and_already_has_a_card(self):
        self.assertTrue(
            pr_triage_decide.decide_remove_pr_card([{"id": "ISSUE_A", "number": 1}], {"id": "PR_ITEM", "status": None, "team": None, "sprint": None})
        )

    def test_false_when_the_pr_closes_an_issue_but_has_no_card_yet(self):
        self.assertFalse(pr_triage_decide.decide_remove_pr_card([{"id": "ISSUE_A", "number": 1}], None))

    def test_false_when_the_pr_closes_no_issue_even_if_it_has_a_card(self):
        self.assertFalse(
            pr_triage_decide.decide_remove_pr_card([], {"id": "PR_ITEM", "status": None, "team": None, "sprint": None})
        )


class DecideIssueCardUpdateTest(unittest.TestCase):
    """Pure decision for one linked issue's card. mapped_team/current_sprint_title
    are only ever consulted when a card is being added (existing_issue_item is
    None); an existing card's Team and Sprint are never touched by any action."""

    EXISTING_WORKING = {"id": "ISSUE_ITEM", "status": "🛠️ Working", "team": "Dev", "sprint": "Sprint 259"}
    EXISTING_BLANK = {"id": "ISSUE_ITEM", "status": None, "team": None, "sprint": None}
    EXISTING_PARKED = {"id": "ISSUE_ITEM", "status": "⛔️ Blocked", "team": "Dev", "sprint": "Sprint 259"}

    # --- opened and the three transitions: Status only on an existing card ---

    def test_opened_and_transitions_set_status_only_on_an_existing_card(self):
        for action in pr_triage_decide.ISSUE_STATUS_ACTIONS:
            with self.subTest(action=action):
                fields = pr_triage_decide.decide_issue_card_update(action, False, self.EXISTING_WORKING, "Delivery", "Sprint 260")
                self.assertEqual(fields, {"status": "Review", "team": None, "sprint": None})

    def test_opened_and_transitions_fill_status_when_the_existing_card_is_blank(self):
        for action in pr_triage_decide.ISSUE_STATUS_ACTIONS:
            with self.subTest(action=action):
                fields = pr_triage_decide.decide_issue_card_update(action, False, self.EXISTING_BLANK, "Delivery", "Sprint 260")
                self.assertEqual(fields, {"status": "Review", "team": None, "sprint": None})

    def test_opened_and_transitions_never_move_a_parked_status(self):
        for action in pr_triage_decide.ISSUE_STATUS_ACTIONS:
            with self.subTest(action=action):
                fields = pr_triage_decide.decide_issue_card_update(action, False, self.EXISTING_PARKED, "Delivery", "Sprint 260")
                self.assertEqual(fields, {"status": None, "team": None, "sprint": None})

    def test_opened_and_transitions_add_and_fill_all_three_on_a_missing_card(self):
        for action in pr_triage_decide.ISSUE_STATUS_ACTIONS:
            with self.subTest(action=action):
                fields = pr_triage_decide.decide_issue_card_update(action, True, None, "Delivery", "Sprint 260")
                self.assertEqual(fields, {"status": "Working", "team": "Delivery", "sprint": "Sprint 260"})

                fields = pr_triage_decide.decide_issue_card_update(action, False, None, "Delivery", "Sprint 260")
                self.assertEqual(fields, {"status": "Review", "team": "Delivery", "sprint": "Sprint 260"})

    # --- synchronize: never touches a linked issue's card, existing or missing ---

    def test_synchronize_never_touches_an_existing_card(self):
        fields = pr_triage_decide.decide_issue_card_update("synchronize", True, self.EXISTING_WORKING, "Delivery", "Sprint 260")
        self.assertIsNone(fields)

    def test_synchronize_never_touches_a_missing_card_either(self):
        fields = pr_triage_decide.decide_issue_card_update("synchronize", True, None, "Delivery", "Sprint 260")
        self.assertIsNone(fields)

    # --- edited: adds and fills Team+Sprint on a missing card, but never
    # touches an existing one, and never writes Status even on the card it adds ---

    def test_edited_never_touches_an_existing_card(self):
        fields = pr_triage_decide.decide_issue_card_update("edited", True, self.EXISTING_WORKING, "Delivery", "Sprint 260")
        self.assertIsNone(fields)

    def test_edited_adds_and_fills_team_and_sprint_on_a_missing_card_but_leaves_status_unwritten(self):
        fields = pr_triage_decide.decide_issue_card_update("edited", True, None, "Delivery", "Sprint 260")
        self.assertEqual(fields, {"status": None, "team": "Delivery", "sprint": "Sprint 260"})

        fields = pr_triage_decide.decide_issue_card_update("edited", False, None, "Delivery", "Sprint 260")
        self.assertEqual(fields, {"status": None, "team": "Delivery", "sprint": "Sprint 260"})


class FindCurrentIterationTest(unittest.TestCase):
    def test_returns_the_iteration_containing_today(self):

        iterations = [
            {"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21},
            {"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21},
        ]

        iteration = pr_triage_decide.find_current_iteration(iterations, datetime.date(2026, 8, 8))

        self.assertEqual(iteration["id"], "bd551114")
        self.assertEqual(iteration["title"], "Sprint 260")

    def test_start_date_itself_is_inside_the_iteration(self):

        iterations = [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}]

        iteration = pr_triage_decide.find_current_iteration(iterations, datetime.date(2026, 8, 3))

        self.assertEqual(iteration["id"], "bd551114")

    def test_the_day_after_the_iteration_ends_is_not_inside_it(self):

        iterations = [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}]

        end_date = datetime.date(2026, 8, 3) + datetime.timedelta(days=21)
        iteration = pr_triage_decide.find_current_iteration(iterations, end_date)

        self.assertIsNone(iteration)

    def test_returns_none_when_no_iteration_contains_the_date(self):

        iterations = [{"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21}]

        iteration = pr_triage_decide.find_current_iteration(iterations, datetime.date(2026, 9, 1))

        self.assertIsNone(iteration)

    def test_returns_none_for_an_empty_iteration_list(self):

        self.assertIsNone(pr_triage_decide.find_current_iteration([], datetime.date(2026, 8, 8)))

    def test_does_not_assume_the_first_entry_is_current(self):

        iterations = [
            {"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21},
            {"id": "bd551113", "title": "Sprint 259", "startDate": "2026-07-13", "duration": 21},
        ]

        iteration = pr_triage_decide.find_current_iteration(iterations, datetime.date(2026, 7, 20))

        self.assertEqual(iteration["id"], "bd551113")


class CheckAssignmentSucceededTest(unittest.TestCase):
    def test_raises_when_author_login_is_absent_from_the_assignees_response(self):
        assign_result = {"assignees": []}

        with self.assertRaises(pr_triage_decide.AssignmentDroppedError) as context:
            pr_triage_decide.check_assignment_succeeded("someuser", assign_result)

        self.assertIn("someuser", str(context.exception))
        self.assertIn("pr-triage-teams.yml", str(context.exception))

    def test_does_not_raise_when_author_login_is_present(self):
        assign_result = {"assignees": [{"login": "someuser"}]}

        pr_triage_decide.check_assignment_succeeded("someuser", assign_result)


class StripEmojiPrefixTest(unittest.TestCase):
    def test_strips_leading_emoji_and_whitespace(self):
        self.assertEqual(pr_triage_decide.strip_emoji_prefix("\U0001f6e0️ Working"), "Working")

    def test_leaves_plain_name_unchanged(self):
        self.assertEqual(pr_triage_decide.strip_emoji_prefix("Icebox"), "Icebox")


class FindOptionIdByNameTest(unittest.TestCase):
    def test_matches_status_option_ignoring_emoji_prefix(self):
        options = [
            {"name": "\U0001f6e0️ Working", "id": "47fc9ee4"},
            {"name": "\U0001f50d Review", "id": "879449e7"},
        ]

        option_id = pr_triage_decide.find_option_id_by_name(options, "Working", strip_emoji=True)

        self.assertEqual(option_id, "47fc9ee4")

    def test_matches_team_option_by_exact_name(self):
        options = [{"name": "Dev", "id": "f2a5529c"}, {"name": "Delivery", "id": "34b176a9"}]

        option_id = pr_triage_decide.find_option_id_by_name(options, "Delivery", strip_emoji=False)

        self.assertEqual(option_id, "34b176a9")

    def test_raises_with_full_option_list_when_name_does_not_resolve(self):
        options = [{"name": "Dev", "id": "f2a5529c"}]

        with self.assertRaises(ValueError) as context:
            pr_triage_decide.find_option_id_by_name(options, "Nonexistent", strip_emoji=False)

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

        mapping = pr_triage_decide.parse_teams_mapping(text)

        self.assertEqual(mapping, {"mswertz": "Dev", "hslh": "Delivery"})

    def test_ignores_blank_lines_and_trailing_comments(self):
        text = (
            "teams:\n"
            "\n"
            "  mswertz: Dev\n"
        )

        mapping = pr_triage_decide.parse_teams_mapping(text)

        self.assertEqual(mapping, {"mswertz": "Dev"})


if __name__ == "__main__":
    unittest.main()
