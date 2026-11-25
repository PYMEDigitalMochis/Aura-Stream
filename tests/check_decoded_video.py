from pathlib import Path
from subprocess import run
from tempfile import TemporaryDirectory
import hashlib, json, shutil
root=Path(__file__).resolve().parents[1]
f=root/'tests/fixtures'
video=f/'sintetico_negro_congelado_15s.mp4'; frames=f/'fotogramas_rgb24_80x45_1fps.raw'
manifest=json.loads((f/'MANIFIESTO.json').read_text())
for p in [video,frames]:
    assert hashlib.sha256(p.read_bytes()).hexdigest()==manifest[p.name],p
assert frames.stat().st_size==15*80*45*3
with TemporaryDirectory() as tmp:
    src=root/'app/src/main/java/mx/detrasdelboton/aura/VideoFrameDetector.java'
    test=root/'tests/TestDecodedFrames.java'
    p=run(['javac','--release','17','-d',tmp,str(src),str(test)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=run(['java','-cp',tmp,'mx.detrasdelboton.aura.TestDecodedFrames',str(frames)],capture_output=True,text=True)
    assert p.returncode==0,p.stdout+'\n'+p.stderr
    print(p.stdout)
if shutil.which('ffprobe'):
    x=run(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(video)],capture_output=True,text=True,check=True)
    assert abs(float(x.stdout.strip())-15)<.1
print('PASS: 2 video candidates from actual FFmpeg-decoded frames; NOT Android decoder')
