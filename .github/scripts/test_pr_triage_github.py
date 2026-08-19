"""Tests for pr_triage_github — the HTTP calls.

A test belongs here when it exercises a request or a mutation, faked at
pr_triage_github.http_request. Decisions are tested in test_pr_triage_decide.py.
"""

import os
import sys
import unittest
from unittest import mock

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pr_triage_github


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
        with mock.patch.object(pr_triage_github, "http_request", return_value=response) as mock_request:
            iterations = pr_triage_github.fetch_project_iterations(pr_triage_github.SPRINT_FIELD_ID, "board-token")

        self.assertEqual(iterations, [{"id": "bd551114", "title": "Sprint 260", "startDate": "2026-08-03", "duration": 21}])
        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertIn("iterations", called_body["query"])
        self.assertNotIn("completedIterations", called_body["query"])


class SetProjectFieldIterationTest(unittest.TestCase):
    def test_writes_the_iteration_id_not_a_single_select_option(self):
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": {}}) as mock_request:
            pr_triage_github.set_project_field_iteration("ITEM_1", pr_triage_github.SPRINT_FIELD_ID, "bd551114", "board-token")

        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertEqual(called_body["variables"]["iterationId"], "bd551114")
        self.assertIn("iterationId", called_body["query"])
        self.assertNotIn("singleSelectOptionId", called_body["query"])


class RemoveItemFromBoardTest(unittest.TestCase):
    """The story's one removal (aim 6): remove_item_from_board, called only
    with the PR's own item id -- never an issue's. The GraphQL mutation
    keeps GitHub's real name, deleteProjectV2Item; only our identifier
    changed (owner ruling: "delete" reads as "delete the pull request")."""

    def test_removes_the_given_item_from_board_15(self):
        with mock.patch.object(
            pr_triage_github, "http_request", return_value={"data": {"deleteProjectV2Item": {"deletedItemId": "PR_ITEM"}}}
        ) as mock_request:
            pr_triage_github.remove_item_from_board("PR_ITEM", "board-token")

        called_body = mock_request.call_args.kwargs.get("body") or mock_request.call_args.args[-1]
        self.assertIn("deleteProjectV2Item", called_body["query"])
        self.assertEqual(called_body["variables"]["projectId"], pr_triage_github.BOARD_PROJECT_ID)
        self.assertEqual(called_body["variables"]["itemId"], "PR_ITEM")
        self.assertEqual(mock_request.call_args.kwargs.get("token") or mock_request.call_args.args[1], "board-token")


class NullNodeIsRaisedActionablyTest(unittest.TestCase):
    def test_fetch_project_field_options_raises_naming_the_field_id_when_node_is_null(self):
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": {"node": None}}):
            with self.assertRaises(pr_triage_github.GraphqlError) as context:
                pr_triage_github.fetch_project_field_options("STALE_FIELD_ID", "board-token")

        self.assertIn("STALE_FIELD_ID", str(context.exception))

    def test_fetch_project_iterations_raises_naming_the_field_id_when_node_is_null(self):
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": {"node": None}}):
            with self.assertRaises(pr_triage_github.GraphqlError) as context:
                pr_triage_github.fetch_project_iterations("STALE_SPRINT_FIELD_ID", "board-token")

        self.assertIn("STALE_SPRINT_FIELD_ID", str(context.exception))


class GraphqlRequestErrorHandlingTest(unittest.TestCase):
    def test_raises_when_response_body_carries_errors_despite_http_200(self):
        errors_payload = [{"message": "Resource not accessible - requires one of the following scopes: ['project']"}]
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": None, "errors": errors_payload}):
            with self.assertRaises(pr_triage_github.GraphqlError) as context:
                pr_triage_github.graphql_request("query {}", {}, "token")

        self.assertIn("requires one of the following scopes", str(context.exception))

    def test_passes_through_a_clean_response(self):
        with mock.patch.object(pr_triage_github, "http_request", return_value={"data": {"ok": True}}):
            result = pr_triage_github.graphql_request("query {}", {}, "token")

        self.assertEqual(result, {"data": {"ok": True}})


if __name__ == "__main__":
    unittest.main()
