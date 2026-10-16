#!/usr/bin/env python3
"""Keep TLP Transfer static inbox in sync with permanently deleted GitHub Issues.

Runs inside GitHub Actions with the temporary GITHUB_TOKEN. Never uses a PAT.
Deletes only the referenced message HTML and index entry, not attachments.
"""
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = Path("ChatGPT/TLP/TLP Transfer")
INDEX = BASE / "index.html"
API = "https://api.github.com/repos/TLP1998/OpenAI/issues/"
RECORD = re.compile(
    r'\{\s*id:"(?P<id>TLP-[^"]+)"[\s\S]*?'
    r'page:"(?P<page>messages/[^"]+\.html)"'
    r'[\s\S]*?issueNumber:(?P<issue>\d+)\s*\}\s*,?'
)
TOKEN = os.getenv("GITHUB_TOKEN", "")


def status_for_issue(number):
    request = urllib.request.Request(
        API + str(number),
        headers={
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            **({"Authorization": "Bearer " + TOKEN} if TOKEN else {}),
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status
    except urllib.error.HTTPError as error:
        return error.code


def main():
    if not TOKEN:
        print("GITHUB_TOKEN missing: skip sync", file=sys.stderr)
        return 1
    html = INDEX.read_text(encoding="utf-8")
    left = html.find("const messages=[")
    right = html.find("];", left)
    if left < 0 or right < 0:
        raise RuntimeError("Could not identify messages array")
    inner_start = left + len("const messages=[")
    body = html[inner_start:right]
    records = list(RECORD.finditer(body))
    print(f"Found {len(records)} transfer messages")
    removed = []
    for match in records:
        issue = match.group("issue")
        status = status_for_issue(issue)
        print(f"{match.group('id')}: issue #{issue} HTTP {status}")
        if status in (404, 410):
            removed.append(match)
        elif status == 200:
            continue
        else:
            print(f"Uncertain status {status} for issue #{issue}; keeping message", file=sys.stderr)
    if not removed:
        print("Nothing to change")
        return 0
    # Reverse edits preserve match offsets.
    for match in reversed(removed):
        body = body[:match.start()] + body[match.end():]
    # Remove stray first comma when a record is removed at the front.
    body = re.sub(r'^\s*,', '', body)
    updated = html[:inner_start] + body + html[right:]
    INDEX.write_text(updated, encoding="utf-8")
    for match in removed:
        filename = BASE / match.group("page")
        # Guard against deleting arbitrary files if index metadata changes.
        if filename.parent != BASE / "messages":
            raise RuntimeError("Unsafe message path")
        if filename.exists():
            filename.unlink()
        print(f"Removed {match.group('id')}: index entry and {filename}")
    print(f"Synced {len(removed)} deleted Issues")
    return 0


if __name__ == "__main__":
    sys.exit(main())
