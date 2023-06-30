# __main__.py

import argparse
import logging
import sys
from getpass import getpass

from .__init__ import OntologyManager

actions = ['add', 'delete', 'update']


def main():
    """Perform an action using the Molgenis Ontology Manager"""

    parser = argparse.ArgumentParser()

    required_args = parser.add_argument_group('Required arguments')
    add_delete_args = parser.add_argument_group('Add/delete arguments')
    update_args = parser.add_argument_group('Update arguments')

    # Required arguments
    required_args.add_argument("action", type=str,
                               help="Supply one of the following actions: add, delete, update")
    required_args.add_argument("table",
                               help="Supply the name of the CatalogueOntologies table"
                                    " on which the action is applied.")

    # Optional arguments for actions 'add' and 'delete'
    add_delete_args.add_argument("-n", "--name", type=str, required=('add' in sys.argv or 'delete' in sys.argv),
                                 help="Fill in the name of the term. Mandatory for add and delete actions.")
    add_delete_args.add_argument("-o", "--order", type=int,
                                 help="Fill in the order of this term, optional.")
    add_delete_args.add_argument("-p", "--parent", type=str,
                                 help="Fill in the parent of this term, optional. "
                                      "Ensure the parent term is present in the table.")
    add_delete_args.add_argument("-l", "--label", type=str,
                                 help="Fill in the label for this term, optional.")
    add_delete_args.add_argument("-d", "--definition", type=str,
                                 help="Fill in the definition for this term, optional.")
    add_delete_args.add_argument("-u", "--URI", type=str,
                                 help="Fill in the ontology term URI for this term, optional.")

    # Arguments for action 'update'
    update_args.add_argument("-old", "--old", type=str, required='update' in sys.argv,
                             help="Fill in the name of the term to be replaced. "
                                  "Mandatory for update action.")
    update_args.add_argument("-new", "--new", type=str, required='update' in sys.argv,
                             help="Fill in the name of the term that replaces the old term. "
                                  "Mandatory for update action.")
    args = parser.parse_args()

    # Read the action that is to be performed from the command line
    if len(sys.argv) > 1:
        action = args.action
        assert action in actions, f"Action {action} is not supported. Select one from {','.join(actions)}."

        url = input("Server url: ")
        username = input("Username: ")
        password = getpass()

        kwargs = {'name': args.name,
                  'label': args.label,
                  'parent': args.parent,
                  'ontologyTermURI': args.URI,
                  'definition': args.definition,
                  'order': args.order,
                  'old': args.old,
                  'new': args.new}

        ontoman = OntologyManager(url, username, password)
        ontoman.perform(action=action, table=args.table, **kwargs)


if __name__ == '__main__':
    logging.basicConfig(level='INFO')
    logging.getLogger("requests").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    main()
