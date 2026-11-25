"""Tests codec zero-length buffers cannot fabricate PCM temporal coverage."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
R=Path(__file__).resolve().parents[1]
J=R/'app/src/main/java/mx/detrasdelboton/aura/AudioSignalDetector.java'
T=R/'tests/TestAudioZeroBuffer.java'
with TemporaryDirectory() as d:
    p=subprocess.run(['javac','--release','17','-Xlint:all','-d',d,str(J),str(T)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=subprocess.run(['java','-cp',d,'mx.detrasdelboton.aura.TestAudioZeroBuffer'],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    print(p.stdout.strip())
