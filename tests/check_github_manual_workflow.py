"""Fail-closed static workflow gate, not proof of a remote GitHub run."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
s=(r/'.github/workflows/android-debug-manual.yml').read_text()
assert 'on:\n  workflow_dispatch:\n  push:\n    branches: ["qa/aura-silence-600ms-20261124"]\n' in s and '  pull_request:' not in s, 'Only manual or exact public QA branch CI allowed'
assert 'contents: read' in s and 'cancel-in-progress: false' in s, 'Workflow permissions/concurrency drift'
assert 'qa-portable:' in s and 'needs: qa-portable' in s, 'Build must depend on QA'
assert 'sh tools/run_qa_isolated.sh' in s, 'Missing portable QA'
assert 'assembleDebug :app:lintDebug' in s and 'check_apk_gate.py --apk' in s, 'Unsigned/unverified APK risk'
assert "gradle-version: '9.6.0'" in s, 'Gradle pinned to AGP compatibility expected'
assert 'build-tools;36.0.0' in s and 'platforms;android-36' in s, 'Android SDK drift'
assert 'retention-days: 3' in s and 'if-no-files-found: error' in s, 'APK artifact gate missing'
assert "versionName '0.1.8-qa'" in (r/'app/build.gradle').read_text(), 'Version mismatch'
assert (r/'tools/check_apk_gate.py').is_file()
print('PASS scoped public QA GitHub Actions, QA dependency, SDK/Gradle, APK verification, no auto-usage (static)')
