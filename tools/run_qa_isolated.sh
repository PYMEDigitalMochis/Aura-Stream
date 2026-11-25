#!/bin/sh
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
python3 "$HERE/tools/verify_manifest.py" "$HERE"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
cp -R "$HERE/." "$TMP/"
cd "$TMP"
for script in check_contract.py check_android_stubs.py check_export.py check_ui.py check_adversarial_ui.py check_webview_lifecycle.py check_android_insets.py check_visual_candidates.py check_decoded_video.py check_native_wiring.py check_prephysical_gates.py check_apk_gate_policy.py check_security_static.py check_pcm_native_contract.py check_pcm_ffmpeg.py check_pcm_property.py check_audio_empty_pts.py check_viewer_no_file_access.py check_github_manual_workflow.py; do
  python3 "tests/$script"
done
python3 "$HERE/tools/verify_manifest.py" "$HERE"
echo 'PASS 19/19 Python entry scripts; QA copy only. No APK, SDK or device tested.'
