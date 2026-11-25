package mx.detrasdelboton.aura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Portable result DTO: only populated by an actual decoder attempting EOS. */
final class CodecAuditResult {
    static final class TrackResult {
        final int index; final boolean audio; final String status, reason;
        final long outputs; final AudioSignalDetector.Result levels;
        TrackResult(int index,boolean audio,String status,String reason,long outputs,AudioSignalDetector.Result levels){
            this.index=index;this.audio=audio;this.status=status;this.reason=reason;this.outputs=outputs;this.levels=levels;
        }
    }
    final String decodeStatus, audioStatus, reason;
    final List<TrackResult> tracks;
    CodecAuditResult(String decode,String audio,String reason,List<TrackResult> tracks){
        this.decodeStatus=decode;this.audioStatus=audio;this.reason=reason;
        this.tracks=Collections.unmodifiableList(new ArrayList<>(tracks));
    }
}
