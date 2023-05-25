# manager.py

class Manager:
    """Class that manages the actions."""

    def __init__(self):
        pass

    def perform(self, action: str, **kwargs):
        """Select the method to perform and pass any keyword arguments"""
        match action:
            case 'add':
                self.add(**kwargs)
            case 'delete':
                self.delete(**kwargs)
            case 'rename':
                self.rename(**kwargs)

    def add(self, **kwargs):
        """Add a term to an ontology."""
        pass

    def delete(self, **kwargs):
        """Delete a term from an ontology."""
        pass

    def rename(self, **kwargs):
        """Rename a term in an ontology."""
        pass
