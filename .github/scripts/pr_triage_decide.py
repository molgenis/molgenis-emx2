"""Every pure verdict PR triage makes, and the vocabulary it compares against.

The decision (decide) is pure: author login + current draft state + the loaded
teams mapping in, an assign/draft/board verdict out. No network, no GitHub
API, no environment reads, no clock, and no import of another pr_triage_*
module -- see pr_triage.current_date for the one place that reads the clock.
"""

import datetime
import re

STATUS_WORKING = "Working"
STATUS_REVIEW = "Review"
UNKNOWN_AUTHOR_TEAM = "Dev"


class AssignmentDroppedError(Exception):
    pass


def check_assignment_succeeded(author_login, assign_result):
    assignee_logins = [assignee["login"] for assignee in assign_result.get("assignees", [])]
    if author_login not in assignee_logins:
        raise AssignmentDroppedError(
            f"GitHub did not assign '{author_login}' (assignees after the call: {assignee_logins}). "
            f"The login is probably no longer valid for assignment; .github/pr-triage-teams.yml is probably stale."
        )


def is_blank(value):
    return not value or not value.strip()


def team_for(author_login, mapping):
    team = mapping.get(author_login)
    return None if is_blank(team) else team


def decide(author_login, is_draft, mapping):
    team = team_for(author_login, mapping)
    if team is None:
        return {"known": False, "force_draft": False, "team": UNKNOWN_AUTHOR_TEAM}
    return {"known": True, "force_draft": not is_draft, "team": team}


def target_status_from_draft_state(is_draft):
    return STATUS_WORKING if is_draft else STATUS_REVIEW


def find_current_iteration(iterations, today):
    for iteration in iterations:
        start = datetime.date.fromisoformat(iteration["startDate"])
        end = start + datetime.timedelta(days=iteration["duration"])
        if start <= today < end:
            return iteration
    return None


STATUS_MANAGED_VALUES = (STATUS_WORKING, STATUS_REVIEW)


def decide_status(is_draft, current_status):
    target = target_status_from_draft_state(is_draft)
    if is_blank(current_status):
        return target
    current = strip_emoji_prefix(current_status)
    if current == target:
        return None
    if current in STATUS_MANAGED_VALUES:
        return target
    return None


def decide_board_update(is_draft, current_status, current_team, mapped_team, current_sprint=None, current_sprint_title=None):
    return {
        "status": decide_status(is_draft, current_status),
        "team": mapped_team if is_blank(current_team) else None,
        "sprint": current_sprint_title if (current_sprint_title and is_blank(current_sprint)) else None,
    }


def keyword_closing_issues(all_closing_issues, user_linked_issues):
    """Aim 6 redirects only on a KEYWORD-derived closing reference
    (close/closes/closed/fix/fixes/fixed/resolve/resolves/resolved in the PR
    body) [owner ruling, narrowing the original "closingIssuesReferences
    reports it, so it counts" reading]. An issue linked only by hand through
    GitHub's Development sidebar, with no keyword in the body, does NOT
    redirect -- `Addresses: <url>` is a real habit in this repo, used
    deliberately to link without auto-closing.

    GitHub's closingIssuesReferences field reports both kinds together;
    userLinkedOnly=true reports only the hand-linked ones. Keyword-derived is
    therefore `all_closing_issues` minus `user_linked_issues`, matched by
    node id -- verified against two real PRs of each shape, see
    notes/github-facts.md §8."""
    user_linked_ids = {issue["id"] for issue in user_linked_issues}
    return [issue for issue in all_closing_issues if issue["id"] not in user_linked_ids]


def decide_redirect_to_issues(closing_issues):
    """Aim 6: a PR that closes at least one issue gets no card of its own --
    every linked issue's card is written instead. An empty list changes
    nothing; the PR is boarded exactly as it always was."""
    return bool(closing_issues)


def decide_remove_pr_card(closing_issues, existing_pr_item):
    """True when the PR closes at least one issue AND already has an item on
    board 15. Board-only: the card comes off board 15, the PR is unchanged.
    Only opened and a body edit reach this; see PR_CARD_REMOVAL_ACTIONS."""
    return decide_redirect_to_issues(closing_issues) and existing_pr_item is not None


def decide_issue_card_update(is_draft, existing_issue_item, mapped_team, current_sprint_title):
    """The story a PR closes is kept on board 15 in the right column: an
    existing card gets its Status moved and nothing else, a missing one is
    added with Status, Team and Sprint."""
    if existing_issue_item is not None:
        return {"status": decide_status(is_draft, existing_issue_item["status"]), "team": None, "sprint": None}
    return {"status": target_status_from_draft_state(is_draft), "team": mapped_team, "sprint": current_sprint_title}


def strip_emoji_prefix(name):
    return re.sub(r"^[^\w]+", "", name).strip()


def find_option_id_by_name(options, target_name, strip_emoji):
    for option in options:
        candidate = strip_emoji_prefix(option["name"]) if strip_emoji else option["name"]
        if candidate == target_name:
            return option["id"]
    available = ", ".join(f'{o["name"]!r} ({o["id"]})' for o in options)
    raise ValueError(
        f"'{target_name}' does not match any live board option. Available options: {available}"
    )


def parse_teams_entries(text):
    entries = []
    in_teams_block = False
    for raw_line in text.splitlines():
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        if line.strip() == "teams:":
            in_teams_block = True
            continue
        if not in_teams_block:
            continue
        if not line.startswith((" ", "\t")):
            in_teams_block = False
            continue
        key, _, value = line.strip().partition(":")
        entries.append((key.strip(), value.strip()))
    return entries


def parse_teams_mapping(text):
    return dict(parse_teams_entries(text))


TRANSITION_ACTIONS = ("ready_for_review", "converted_to_draft", "reopened")
BOARD_UPDATE_ACTIONS = TRANSITION_ACTIONS + ("edited",)
PR_CARD_REMOVAL_ACTIONS = ("opened", "edited")
