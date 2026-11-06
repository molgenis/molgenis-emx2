"""Checks for the 10.0 migration of 'contact email'.
Run from this folder with: python test_update_10_0.py
"""
from update.update_10_0 import move_contact_emails


def contact_point(resource):
    return resource['contact point.first name'], resource['contact point.last name']


def test_email_of_existing_contact_becomes_contact_point():
    resources = [{'id': 'GenR', 'contact email': 'v.jaddoe@erasmusmc.nl'}]
    contacts = [{'resource': 'GenR', 'first name': 'Vincent', 'last name': 'Jaddoe', 'email': 'V.Jaddoe@erasmusmc.nl'}]

    move_contact_emails(resources, contacts)

    assert 'contact email' not in resources[0]
    assert contact_point(resources[0]) == ('Vincent', 'Jaddoe')
    assert len(contacts) == 1


def test_unknown_email_becomes_new_contact():
    resources = [{'id': 'HPV', 'contact email': 'tiina.eriksson@tuni.fi'}]
    contacts = []

    move_contact_emails(resources, contacts)

    assert contact_point(resources[0]) == ('Tiina', 'Eriksson')
    assert contacts[0]['email'] == 'tiina.eriksson@tuni.fi'
    assert contacts[0]['role'] == 'Primary contact'


def test_email_is_added_to_contact_with_same_name_and_no_email():
    resources = [{'id': 'HPV', 'contact email': 'tiina.eriksson@tuni.fi'}]
    contacts = [{'resource': 'HPV', 'first name': 'Tiina', 'last name': 'Eriksson', 'email': ''}]

    move_contact_emails(resources, contacts)

    assert len(contacts) == 1
    assert contacts[0]['email'] == 'tiina.eriksson@tuni.fi'


def test_mailbox_becomes_contact_with_placeholder_name():
    resources = [{'id': 'CSC', 'contact email': 'info@nkcx.se'}]
    contacts = []

    move_contact_emails(resources, contacts)

    assert contact_point(resources[0]) == ('Contact', 'info')


def test_two_emails_give_one_contact_point_and_one_extra_contact():
    resources = [{'id': 'CON', 'contact email': 'frtm@ssi.dk, mineindkob@ssi.dk'}]
    contacts = []

    move_contact_emails(resources, contacts)

    assert contact_point(resources[0]) == ('Contact', 'frtm')
    assert [contact['email'] for contact in contacts] == ['frtm@ssi.dk', 'mineindkob@ssi.dk']


def test_existing_contact_point_is_kept():
    resources = [{'id': 'LSC', 'contact email': 'martin.widschwendter@uibk.ac.at',
                  'contact point.resource': 'LSC', 'contact point.first name': 'Anna', 'contact point.last name': 'Smit'}]
    contacts = [{'resource': 'LSC', 'first name': 'Anna', 'last name': 'Smit', 'email': 'anna@uibk.ac.at'}]

    move_contact_emails(resources, contacts)

    assert contact_point(resources[0]) == ('Anna', 'Smit')
    assert contacts[1]['email'] == 'martin.widschwendter@uibk.ac.at'


def test_same_name_with_other_email_does_not_give_duplicate_contact():
    resources = [{'id': 'HPV', 'contact email': 'tiina.eriksson@tuni.fi'}]
    contacts = [{'resource': 'HPV', 'first name': 'Tiina', 'last name': 'Eriksson', 'email': 'tiina@other.fi'}]

    move_contact_emails(resources, contacts)

    assert len(contacts) == 2
    assert contact_point(resources[0]) == ('Contact', 'tiina.eriksson@tuni.fi')


def test_resource_without_email_does_not_change():
    resources = [{'id': 'MAT', 'contact email': ''}]
    contacts = []

    move_contact_emails(resources, contacts)

    assert resources == [{'id': 'MAT'}]
    assert contacts == []


if __name__ == '__main__':
    tests = [test for name, test in list(globals().items()) if name.startswith('test_')]
    for test in tests:
        test()
        print('OK  ', test.__name__)
    print(f'All {len(tests)} checks passed')
