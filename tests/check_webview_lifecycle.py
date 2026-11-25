"""Structural regression gate; a JVM stub is NOT a device or Android SDK."""
from pathlib import Path
src=(Path(__file__).resolve().parents[1]/'app/src/main/java/mx/detrasdelboton/aura/MainActivity.java').read_text()
body=src.split('private void releaseViewer(){',1)[1].split('private void showHome(){',1)[0]
assert body.index('removeView(previous)')<body.index('previous.destroy()'), 'WebView destroyed while attached'
assert 'if(view==web){showHome();' in src, 'renderer crash does not release via showHome'
assert 'if(!destroyed && view==web && HOME.equals(url))' in src, 'stale WebView can receive report'
assert 's.setSupportMultipleWindows(false)' in src
assert 's.setGeolocationEnabled(false)' in src
assert 's.setJavaScriptCanOpenWindowsAutomatically(false)' in src
assert src.count('releaseViewer();')>=3
print('PASS: WebView detached before destruction, stale viewer guarded; windows/location denied')
