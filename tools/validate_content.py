"""Validate content assets by running the Kotlin ContentAssetsTest (single source of validation rules).

Usage: python tools/validate_content.py
"""

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> int:
    gradlew = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    cmd = [str(gradlew), ":app:testDebugUnitTest", "--tests", "*ContentAssetsTest"]
    return subprocess.run(cmd, cwd=ROOT).returncode


if __name__ == "__main__":
    sys.exit(main())
