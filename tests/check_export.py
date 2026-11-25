from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
root=Path(__file__).resolve().parents[1]
with TemporaryDirectory() as tmp:
 src=root/'app/src/main/java/mx/detrasdelboton/aura/ReportContract.java'
 test=root/'tests/TestExportVerification.java'
 subprocess.run(['javac','--release','17','-d',tmp,str(src),str(root/'app/src/main/java/mx/detrasdelboton/aura/VideoFrameDetector.java'),str(src.parent/'AudioSignalDetector.java'),str(src.parent/'CodecAuditResult.java'),str(test)],check=True)
 subprocess.run(['java','-cp',tmp,'TestExportVerification'],check=True)
