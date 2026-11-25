"""Independent FFmpeg AAC fixture, then portable JVM signal inspection."""
from pathlib import Path
from tempfile import TemporaryDirectory
from subprocess import run
from math import sin,pi
import wave,array,sys
ROOT=Path(__file__).resolve().parents[1]
J=ROOT/'app/src/main/java/mx/detrasdelboton/aura'
with TemporaryDirectory() as tmp:
    out=Path(tmp)
    wav=out/'golden.wav'
    n=16000
    pcm=array.array('h',(round(8191*sin(2*pi*440*i/n)) if (i<n or i>=n*2) else 0 for i in range(n*3)))
    if sys.byteorder!='little':pcm.byteswap()
    with wave.open(str(wav),'wb') as w:
        w.setnchannels(1);w.setframerate(n);w.setsampwidth(2);w.writeframes(pcm.tobytes())
    mp4=out/'fixture.m4a';raw=out/'decoded.pcm'
    p=run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','aac','-b:a','96k',str(mp4)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-i',str(mp4),'-ar','16000','-ac','1','-f','s16le',str(raw)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    src=[J/'AudioSignalDetector.java',ROOT/'tests/TestAudioFixture.java']
    p=run(['javac','--release','17','-Xlint:all','-d',str(out),*map(str,src)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=run(['java','-cp',str(out),'mx.detrasdelboton.aura.TestAudioFixture',str(raw),str(n)],capture_output=True,text=True)
    assert p.returncode==0,p.stdout+p.stderr
    print(p.stdout.strip())
print('PASS FFmpeg synthetic AAC clip, decoded PCM validated by pure Java; Android MediaCodec not exercised')
