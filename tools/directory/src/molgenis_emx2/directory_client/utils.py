import json
from collections import OrderedDict
from typing import List


class MolgenisRequestError(Exception):
    def __init__(self, error, response=False):
        self.message = error
        if response:
            self.response = response


def raise_exception(ex):
    """Raises an exception with error message from molgenis"""
    message = ex.args[0]
    if ex.response.content:
        try:
            error = json.loads(ex.response.content.decode("utf-8"))["errors"][0][
                "message"
            ]
        except ValueError:  # Cannot parse JSON
            error = ex.response.content
        except KeyError:  # Cannot parse JSON
            error = json.loads(ex.response.content.decode("utf-8"))["detail"]
        error_msg = "{}: {}".format(message, error)
        raise MolgenisRequestError(error_msg, ex.response)
    else:
        raise MolgenisRequestError("{}".format(message))


def to_ordered_dict(rows: List[dict]) -> OrderedDict:
    rows_by_id = OrderedDict()
    for row in rows:
        rows_by_id[row["id"]] = row
    return rows_by_id
