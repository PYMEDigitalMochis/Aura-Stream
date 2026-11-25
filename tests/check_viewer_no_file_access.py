"""Static security invariants; does not claim runtime Android WebView testing."""
from pathlib import Path
S=(Path(__file__).resolve().parents[1]/'app/src/main/java/mx/detrasdelboton/aura/MainActivity.java').read_text()
assert 's.setAllowContentAccess(false)' in S
assert 's.setAllowFileAccess(false)' in S
assert 'onShowFileChooser(' not in S
assert 'PICK_WEB_FILE' not in S
assert 'onReceiveValue(' not in S
assert 'WebViewAssetLoader.AssetsPathHandler(this)' in S
assert 'window.__auraAndroidReportBase64' in S
assert "'reportFile', 'export'" in (Path(__file__).resolve().parents[1]/'app/src/main/assets/android_poc_ui.js').read_text()
print('PASS viewer read-only native report; content and file chooser disabled (static)')
