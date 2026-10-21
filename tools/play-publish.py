#!/usr/bin/env python3
"""Pushes the store listings in docs/play-store/ to Google Play through the Play Developer API.

Auth is keyless: you log in with `gcloud auth login`, and the script asks IAM for a one-hour token of the
publishing service account (your account needs roles/iam.serviceAccountTokenCreator on it, and the service
account needs app permissions in Play Console → Users and permissions).

Usage: tools/play-publish.py --status              # show listings and tracks currently on Play
       tools/play-publish.py --validate            # upload everything into an edit, validate it, then discard it
       tools/play-publish.py                       # upload text, screenshots and graphics, then commit
       tools/play-publish.py --text-only --only vi,ja-JP
       tools/play-publish.py --bundle app/build/outputs/bundle/release/app-release.aab --notes "Bug fixes."
                                                   # upload an AAB and roll it out to the internal track
Forks: pass --package and --service-account (or set PLAY_SERVICE_ACCOUNT).
"""
import argparse
import importlib.util
import io
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
STORE = ROOT / "docs/play-store"
API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications"
UPLOAD = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications"
DEFAULT_SA = "mockgps-publisher@dsai-play-publisher.iam.gserviceaccount.com"
DETAILS = {"defaultLanguage": "en-US", "contactEmail": "loivo@dsai.vn", "contactWebsite": "https://dsai.vn"}
# listing file stem → (Play language code, screenshots folder)
LANGS = {
    "en-US": ("en-US", "en-US"), "vi-VN": ("vi", "vi-VN"), "es-ES": ("es-ES", "es-ES"), "pt-BR": ("pt-BR", "pt-BR"),
    "id": ("id", "in-ID"), "hi-IN": ("hi-IN", "hi-IN"), "ja-JP": ("ja-JP", "ja-JP"), "ko-KR": ("ko-KR", "ko-KR"),
    "zh-CN": ("zh-CN", "zh-CN"), "zh-TW": ("zh-TW", "zh-TW"), "ru-RU": ("ru-RU", "ru-RU"), "de-DE": ("de-DE", "de-DE"),
    "fr-FR": ("fr-FR", "fr-FR"), "tr-TR": ("tr-TR", "tr-TR"), "th": ("th", "th-TH"),
}


class ApiError(Exception):
    pass


def call(method, url, token, body=None, data=None, content_type="application/json"):
    if body is not None:
        data = json.dumps(body).encode()
    req = urllib.request.Request(url, method=method, data=data,
                                 headers={"Authorization": f"Bearer {token}", "Content-Type": content_type})
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        try:
            msg = json.loads(e.read())["error"]["message"]
        except Exception:
            msg = e.reason
        raise ApiError(f"{method} {url.split('/applications/')[-1]} → {e.code}: {msg}") from None


def access_token(service_account):
    gcloud = shutil.which("gcloud") or os.path.expanduser("~/google-cloud-sdk/bin/gcloud")
    user = subprocess.check_output([gcloud, "auth", "print-access-token"], text=True).strip()
    r = call("POST", f"https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/{service_account}:generateAccessToken",
             user, {"scope": ["https://www.googleapis.com/auth/androidpublisher"], "lifetime": "3600s"})
    return r["accessToken"]


def load_listing(stem):
    spec = importlib.util.spec_from_file_location("check_translations", ROOT / "tools/check-translations.py")
    checker = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(checker)
    path = STORE / "listing" / f"{stem}.md"
    errors = checker.check_listing(path)
    if errors:
        sys.exit(f"listing/{path.name}: {'; '.join(errors)}")
    name, short, full = (p.strip() for p in re.split(r"^## .*$", path.read_text(encoding="utf-8"), flags=re.M)[1:4])
    return {"title": name, "shortDescription": short, "fullDescription": full}


def png(path, mode):
    """Play wants 24-bit PNGs for screenshots and the feature graphic and a 32-bit PNG icon; ours are palette-compressed."""
    out = io.BytesIO()
    Image.open(path).convert(mode).save(out, "PNG", optimize=True)
    return out.getvalue()


def replace_images(base, token, lang, image_type, files, mode):
    call("DELETE", f"{base}/listings/{lang}/{image_type}", token)
    upload = base.replace(API, UPLOAD)
    for f in files:
        call("POST", f"{upload}/listings/{lang}/{image_type}?uploadType=media", token, data=png(f, mode), content_type="image/png")


def release_bundle(base, token, path, track, notes):
    with open(path, "rb") as f:
        bundle = call("POST", f"{base.replace(API, UPLOAD)}/bundles?uploadType=media", token,
                      data=f.read(), content_type="application/octet-stream")
    code = str(bundle["versionCode"])
    release = {"versionCodes": [code], "status": "completed",
               "releaseNotes": [{"language": DETAILS["defaultLanguage"], "text": notes}]}
    call("PUT", f"{base}/tracks/{track}", token, {"track": track, "releases": [release]})
    print(f"ok  versionCode {code} → {track}")


def status(base, token):
    details = call("GET", f"{base}/details", token)
    print("details:", details)
    for l in call("GET", f"{base}/listings", token).get("listings", []):
        shots = call("GET", f"{base}/listings/{l['language']}/phoneScreenshots", token).get("images", [])
        print(f"  {l['language']:6} {l.get('title', '')!r} · {len(shots)} screenshots")
    for t in call("GET", f"{base}/tracks", token).get("tracks", []):
        print(f"track {t['track']}:", [(r.get("name"), r.get("versionCodes"), r.get("status")) for r in t.get("releases", [])])


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--package", default="vn.dsai.mockgps")
    ap.add_argument("--service-account", default=os.environ.get("PLAY_SERVICE_ACCOUNT", DEFAULT_SA))
    ap.add_argument("--only", help="comma-separated listing names or Play codes, e.g. en-US,vi")
    ap.add_argument("--text-only", action="store_true", help="skip screenshots and graphics")
    ap.add_argument("--status", action="store_true", help="print what is on Play now and exit")
    ap.add_argument("--validate", action="store_true", help="validate the edit, then discard it instead of committing")
    ap.add_argument("--bundle", help="upload this AAB to --track instead of pushing the listings")
    ap.add_argument("--track", default="internal", help="track for --bundle (the service account can't release to production)")
    ap.add_argument("--notes", default="Bug fixes and improvements.", help="en-US release notes for --bundle")
    ap.add_argument("--no-review", action="store_true", help="commit with changesNotSentForReview (send from Console later)")
    args = ap.parse_args()

    stems = list(LANGS)
    if args.only:
        wanted = set(args.only.split(","))
        stems = [s for s in stems if s in wanted or LANGS[s][0] in wanted]
        if not stems:
            sys.exit(f"no listing matches {args.only}")
    listings = {} if args.bundle else {s: load_listing(s) for s in stems}

    token = access_token(args.service_account)
    edit = call("POST", f"{API}/{args.package}/edits", token, {})["id"]
    base = f"{API}/{args.package}/edits/{edit}"
    try:
        if args.status:
            status(base, token)
            return
        if args.bundle:
            release_bundle(base, token, args.bundle, args.track, args.notes)
            stems = []
        else:
            call("PATCH", f"{base}/details", token, DETAILS)
        for stem in stems:
            lang, shots_dir = LANGS[stem]
            call("PUT", f"{base}/listings/{lang}", token, {"language": lang, **listings[stem]})
            msg = "text"
            if not args.text_only:
                shots = sorted((STORE / "graphics/screenshots" / shots_dir).glob("*.png"))
                replace_images(base, token, lang, "phoneScreenshots", shots, "RGB")
                msg += f" + {len(shots)} screenshots"
                if lang == DETAILS["defaultLanguage"]:
                    replace_images(base, token, lang, "icon", [STORE / "graphics/icon-512.png"], "RGBA")
                    replace_images(base, token, lang, "featureGraphic", [STORE / "graphics/feature-graphic.png"], "RGB")
                    msg += " + icon + feature graphic"
            print(f"ok  {lang:6} {msg}")
        call("POST", f"{base}:validate", token)
        if args.validate:
            print("edit is valid (discarded, nothing published)")
            return
        query = "?changesNotSentForReview=true" if args.no_review else ""
        call("POST", f"{base}:commit{query}", token)
        print("committed")
        edit = None
    except ApiError as e:
        sys.exit(f"error: {e}")
    finally:
        if edit:
            try:
                call("DELETE", base, token)
            except ApiError:
                pass


if __name__ == "__main__":
    main()
