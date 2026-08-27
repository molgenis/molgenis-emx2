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
    """The story's one removal: unconditional, and narrow. True exactly when
    the PR closes at least one issue AND the PR already has an item on board
    15 -- never guarded by that item's Status or fields, and never true for
    an issue's item, which this is never called with. "Removal" is
    board-only: the card comes off board 15, nothing about the PR itself
    changes.

    Deliberately action-blind: this function does not know synchronize is
    excluded from removal. That exclusion lives in the one call site,
    apply_board_write's `action not in PR_CARD_REMOVAL_ACTIONS` guard --
    apply_redirected_board_write (and this function) are never reached for
    synchronize at all. If a second caller is ever added, that guard has to
    move or be duplicated; it does not enforce itself here."""
    return decide_redirect_to_issues(closing_issues) and existing_pr_item is not None


def decide_issue_card_update(action, is_draft, existing_issue_item, mapped_team, current_sprint_title):
    """Pure decision for ONE linked issue's card. existing_issue_item is None
    when the issue has no item yet on board 15.

    - `synchronize` never touches a linked issue's card at all -- existing or
      missing, this returns None.
    - `edited` never touches an EXISTING issue card (Status is not moved by a
      body edit), but still adds and fills a missing one, because a PR with
      no card and an issue with no card is the one outcome no aim tolerates.
      Status is left unwritten on the card it adds -- only opened and the
      three transitions ever move a linked issue's Status.
    - `opened` and the three transitions set Status only on an existing card
      (Team and Sprint stay exactly as a person left them), and fill all
      three -- Status, Team, Sprint -- on a card they add.

    Returns None for "write nothing", otherwise a fields dict shaped like
    resolve_and_apply_board_fields expects: {"status", "team", "sprint"},
    each either a target value or None meaning "leave as-is"."""
    if existing_issue_item is not None:
        if action not in ISSUE_STATUS_ACTIONS:
            return None
        return {"status": decide_status(is_draft, existing_issue_item["status"]), "team": None, "sprint": None}

    if action not in ISSUE_STATUS_ACTIONS and action not in ISSUE_ADD_ONLY_ACTIONS:
        return None
    status = target_status_from_draft_state(is_draft) if action in ISSUE_STATUS_ACTIONS else None
    return {"status": status, "team": mapped_team, "sprint": current_sprint_title}


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
BOARD_UPDATE_ACTIONS = TRANSITION_ACTIONS + ("synchronize", "edited")

# Aim 6, redirected to a linked issue's card instead of the PR's own: which
# actions may set that card's Status, and which may only add+fill a missing
# one without ever moving Status. See decide_issue_card_update.
ISSUE_STATUS_ACTIONS = TRANSITION_ACTIONS + ("opened",)
ISSUE_ADD_ONLY_ACTIONS = ("edited",)

# The actions that remove the PR's own card when it closes an issue: opened,
# edited, and the three transitions. NOT synchronize -- a push that removed
# the PR's card while never adding the issue's would leave zero cards for
# the same piece of work (owner ruling, review finding F1). For a closing
# PR, synchronize therefore writes nothing anywhere: not the PR's card
# (removal or otherwise), not any issue's.
PR_CARD_REMOVAL_ACTIONS = ISSUE_STATUS_ACTIONS + ISSUE_ADD_ONLY_ACTIONS
