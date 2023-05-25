# manager.py

class Manager:
    """Class that manages the actions."""

    def __init__(self):
        pass

    def perform(self, action: str, table: str, **kwargs):
        """Select the method to perform and pass any keyword arguments"""
        match action:
            case 'add':
                self.add(table, **kwargs)
            case 'delete':
                self.delete(table, **kwargs)
            case 'rename':
                self.rename(table, **kwargs)

    def add(self, table: str, **kwargs):
        """Add a term to an ontology."""
        print(f"Adding to table {table}.")
        pass

    def delete(self, table: str, **kwargs):
        """Delete a term from an ontology."""
        print(f"Deleting from table {table}.")
        pass

    def rename(self, table: str, **kwargs):
        """Rename a term in an ontology."""
        print(f"Renaming in table {table}.")
        pass
