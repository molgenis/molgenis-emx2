from pathlib import Path

(Path(__file__).parent.parent / "pet.csv").unlink(missing_ok=True)
