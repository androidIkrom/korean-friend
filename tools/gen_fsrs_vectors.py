"""Generate FSRS test vectors from py-fsrs for the Kotlin port in :core:srs.

Usage: python tools/gen_fsrs_vectors.py
Writes core/srs/src/test/resources/fsrs_vectors.json.
"""

import json
from datetime import datetime, timezone
from importlib.metadata import version
from pathlib import Path

from fsrs import Card, Rating, Scheduler

SEQUENCES = [
    [3, 3, 3, 3, 3],
    [1, 1, 3, 3, 4],
    [4, 4, 4],
    [3, 3, 1, 3, 3, 3],
    [2, 2, 3, 1, 3],
    [3, 3, 3, 1, 1, 3, 4],
]
START = datetime(2026, 1, 1, tzinfo=timezone.utc)


def main() -> None:
    scheduler = Scheduler(enable_fuzzing=False)
    cases = []
    for ratings in SEQUENCES:
        card = Card(due=START)
        steps = []
        for r in ratings:
            now = card.due
            card, _ = scheduler.review_card(card, Rating(r), now)
            steps.append(
                {
                    "rating": r,
                    "reviewed": int(now.timestamp()),
                    "state": int(card.state),
                    "step": card.step,
                    "stability": card.stability,
                    "difficulty": card.difficulty,
                    "due": int(card.due.timestamp()),
                }
            )
        cases.append({"start": int(START.timestamp()), "steps": steps})
    out = Path(__file__).resolve().parent.parent / "core/srs/src/test/resources/fsrs_vectors.json"
    out.write_text(json.dumps({"fsrs_version": version("fsrs"), "cases": cases}, indent=2) + "\n")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
