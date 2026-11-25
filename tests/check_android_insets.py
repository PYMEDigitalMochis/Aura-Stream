"""Regression gate: both Android roots receive status/navigation/cutout safe-area padding.
Actual clipping is additionally checked by screenshot on an Android emulator.
"""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/'app/src/main/java/mx/detrasdelboton/aura/MainActivity.java').read_text()
assert 'ScrollView scroll=new ScrollView(this);scroll.setFitsSystemWindows(true);' in s
assert 'web=new WebView(this);web.setFitsSystemWindows(true);setContentView(web);' in s
print('PASS native ScrollView and offline WebView request system-window insets (device verification separate)')
