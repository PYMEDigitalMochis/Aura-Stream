from pathlib import Path
from tempfile import TemporaryDirectory
from subprocess import run
import re
R=Path(__file__).resolve().parents[1]
J=R/'app/src/main/java/mx/detrasdelboton/aura'
with TemporaryDirectory() as t:
 p=run(['javac','--release','17','-Xlint:all','-d',t,str(J/'VideoFrameDetector.java'),str(R/'tests/TestVisualDeadline.java')],text=True,capture_output=True)
 assert p.returncode==0,p.stderr
 p=run(['java','-cp',t,'mx.detrasdelboton.aura.TestVisualDeadline'],text=True,capture_output=True)
 assert p.returncode==0,p.stderr
 print(p.stdout.strip())
main=(J/'MainActivity.java').read_text()
meta=(J/'MetadataProbe.java').read_text()
sampler=(J/'AndroidFrameSampler.java').read_text()
assert 'r.visualStatus' in main and 'r.visualCandidateCount' in main and 'r.visualReason' in main
assert 'final String visualStatus, visualReason;' in meta
assert 'visual.status,visual.reason,visual.findings.size(),codec.decodeStatus,codec.audioStatus,summary.toString()' in meta
assert '((long)width)*height>20000' in sampler
assert "versionName '0.1.8-qa'" in (R/'app/build.gradle').read_text()
assert 'no se generó reporte' in meta
assert 'status' in (J/'ReportContract.java').read_text()
print('PASS visual status and UI cannot misrepresent a skipped detector')
