"""Validate the signed logo release without accessing signing key material."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import subprocess
import zipfile
from PIL import Image

CERT_SHA256 = 'a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc'


def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.STDOUT)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', required=True)
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--java', default='java')
    parser.add_argument('--report', required=True)
    args = parser.parse_args()
    signature = run(args.java, '-jar', 'signer/apksigner.jar', 'verify',
                    '--verbose', '--print-certs', args.apk)
    require(f'certificate SHA-256 digest: {CERT_SHA256}' in signature,
            'Signing certificate differs from the previous version 4 APK')
    for scheme in ('v1', 'v2', 'v3'):
        require(re.search(rf'Verified using {scheme} scheme .*: true', signature),
                f'Missing valid {scheme} signature')
    badging = run(args.aapt, 'dump', 'badging', args.apk)
    require("package: name='net.velalab.youtube' versionCode='5' versionName='0.1.4'" in badging,
            'Incorrect package or version')
    require("sdkVersion:'21'" in badging, 'Minimum Android version changed')
    require('leanback-launchable-activity:' in badging, 'Missing Android TV launcher')
    manifest = run(args.aapt, 'dump', 'xmltree', args.apk, 'AndroidManifest.xml')
    for name in ('android:icon', 'android:roundIcon', 'android:banner'):
        require(name in manifest, f'Missing {name}')
    resources = []
    with zipfile.ZipFile(args.apk) as apk:
        names = apk.namelist()
        for density, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96),
                              ('xxhdpi', 144), ('xxxhdpi', 192)]:
            matches = [n for n in names if re.fullmatch(
                rf'res/mipmap-{density}(?:-v\d+)?/app_icon.png', n)]
            require(len(matches) == 1, f'Missing/ambiguous {density} PNG')
            resources.append((matches[0], size, size))
        banners = [n for n in names if re.fullmatch(
            r'res/drawable-xhdpi(?:-v\d+)?/vela_youtube_tv_banner.png', n)]
        require(len(banners) == 1, 'TV banner PNG missing')
        resources.append((banners[0], 320, 180))
        logos = [n for n in names if n.endswith('/vela_youtube_logo.png')]
        require(len(logos) == 1, 'Adaptive foreground PNG missing')
        resources.append((logos[0], 1024, 1024))
        for path, width, height in resources:
            data = apk.read(path)
            require(data[:8] == b'\x89PNG\r\n\x1a\n', f'Invalid PNG: {path}')
            require(struct.unpack('>II', data[16:24]) == (width, height),
                    f'Incorrect PNG dimensions: {path}')
            source = Path('app/src/main/res') / re.sub(r'-v\d+(?=/)', '', path[4:])
            with Image.open(io.BytesIO(data)) as compiled, Image.open(source) as original:
                require(compiled.convert('RGBA').tobytes() == original.convert('RGBA').tobytes(),
                        f'Compiled PNG pixels differ from supplied artwork: {path}')
        adaptive = [n for n in names if re.fullmatch(
            r'res/mipmap-anydpi-v26/app_icon.xml', n)]
        require(len(adaptive) == 1, 'Adaptive icon missing')
        xml = run(args.aapt, 'dump', 'xmltree', args.apk, adaptive[0])
        require('adaptive-icon' in xml and 'foreground' in xml, 'Invalid adaptive icon')
        require('monochrome' not in xml, 'Stale monochrome icon')
    report = dict(package='net.velalab.youtube', versionCode=5, versionName='0.1.4',
                  certificateSha256=CERT_SHA256, signatureSchemes=['v1', 'v2', 'v3'],
                  apkSha256=hashlib.sha256(Path(args.apk).read_bytes()).hexdigest(),
                  pngResources=[dict(path=p, width=w, height=h) for p, w, h in resources],
                  androidTvLauncher=True, adaptiveIcon=True, roundIcon=True,
                  signatureOutput=signature)
    Path(args.report).write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
