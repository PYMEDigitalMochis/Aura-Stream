from pathlib import Path
import subprocess,tempfile
root=Path(__file__).resolve().parents[1]
java=root/'app/src/main/java/mx/detrasdelboton/aura/AudioSignalDetector.java'
test=root/'tests/TestAudioProperty.java'
with tempfile.TemporaryDirectory() as d:
    p=subprocess.run(['javac','--release','17','-Xlint:all','-d',d,str(java),str(test)],text=True,capture_output=True)
    assert p.returncode==0,p.stderr
    p=subprocess.run(['java','-ea','-cp',d,'mx.detrasdelboton.aura.TestAudioProperty'],text=True,capture_output=True)
    assert p.returncode==0,p.stderr
    print(p.stdout.strip())
