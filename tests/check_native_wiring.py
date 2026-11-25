from pathlib import Path
r=Path(__file__).resolve().parents[1]
a=(r/'app/src/main/java/mx/detrasdelboton/aura/MetadataProbe.java').read_text()
b=(r/'app/src/main/java/mx/detrasdelboton/aura/AndroidFrameSampler.java').read_text()
c=(r/'app/src/main/java/mx/detrasdelboton/aura/MainActivity.java').read_text()
assert 'AndroidFrameSampler.inspect(resolver,uri,durationMs/1000.0)' in a
assert 'digest(resolver,uri)' in a and a.index('AndroidFrameSampler.inspect') < a.rindex('digest(resolver,uri)')
assert 'getScaledFrameAtTime(timeUs,MediaMetadataRetriever.OPTION_CLOSEST,80,45)' in b
assert 'bmp.recycle()' in b
assert 'if(export!=null)export.setEnabled(report!=null)' in c
assert 'if(currentProbe!=null)currentProbe.cancel(true)' in c
assert 'void cancelProbe()' in c and '++requestVersion' in c
assert 'source_unchanged' in (r/'app/src/main/java/mx/detrasdelboton/aura/ReportContract.java').read_text()
codec=(r/'app/src/main/java/mx/detrasdelboton/aura/AndroidCodecAuditor.java').read_text()
assert 'inputFormat.getLong(MediaFormat.KEY_DURATION)' in codec
assert 'meter.finish(expectedAudioSeconds)' in codec
assert 'MAX_PIXELS=1920L*1080' in codec
assert 'diagnostic(e)' in codec and 'e.getMessage()' in codec, 'Codec failure must preserve diagnostic message'
print('PASS static wiring: decode sample, per-track audio coverage, 1080p guard, integrity recheck')
