from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

(Path(__file__).parent / "pet.csv").unlink(missing_ok=True)
(Path(__file__).parent / "pet.xlsx").unlink(missing_ok=True)
(Path(__file__).parent / "pet store.zip").unlink(missing_ok=True)
(Path(__file__).parent / "pet store.xlsx").unlink(missing_ok=True)
(Path(__file__).parent / "pet store.json").unlink(missing_ok=True)
(Path(__file__).parent / "pet store.yaml").unlink(missing_ok=True)
