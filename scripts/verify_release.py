"""Validate the release candidate without accessing signing key material."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile
from PIL import Image

INSECURE_AOSP_TEST_CERT_SHA256 = 'a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc'
APPROVED_VELALAB_CERT_SHA256 = 'f3e5a2d996567a1e635f7c674e3f122ff3c6e599af9a0b5aa12bf079c706f904'


def run(*args):
    return subprocess.check_output(args, text=True, encoding='utf-8', stderr=subprocess.STDOUT)


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def resource_files(table):
    """Resolve AAPT's shortened APK paths through named resource configurations."""
    result = {}
    config = resource = None
    for line in table.splitlines():
        match = re.match(r'\s*config (.*):$', line)
        if match:
            config = match[1]
        match = re.match(r'\s*resource (0x[0-9a-f]+) [^:]+:([^:]+):', line)
        if match:
            resource = match[2]
        match = re.match(r'\s*\(string8\) "(res/[^"]+)"', line)
        if match and resource:
            result[resource, config] = match[1]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', required=True)
    parser.add_argument('--aapt', required=True)
    parser.add_argument('--apksigner', required=True)
    parser.add_argument('--report', required=True)
    args = parser.parse_args()
    cert_sha256 = os.environ.get('VELA_RELEASE_CERT_SHA256', '').strip().lower()
    require(re.fullmatch(r'[0-9a-f]{64}', cert_sha256),
            'VELA_RELEASE_CERT_SHA256 must contain the independently verified keystore certificate SHA-256')
    require(cert_sha256 != INSECURE_AOSP_TEST_CERT_SHA256,
            'AOSP public test certificate is not permitted for this release')
    require(cert_sha256 == APPROVED_VELALAB_CERT_SHA256,
            'Release certificate differs from the approved VelaLab identity')
    signature = run(args.apksigner, 'verify', '--verbose', '--print-certs', args.apk)
    require(f'certificate SHA-256 digest: {cert_sha256}' in signature,
            'APK certificate differs from the verified permanent keystore')
    for scheme in ('v1', 'v2', 'v3'):
        require(re.search(rf'Verified using {scheme} scheme .*: true', signature),
                f'Missing valid {scheme} signature')
    badging = run(args.aapt, 'dump', 'badging', args.apk)
    require("package: name='net.velalab.youtube' versionCode='7' versionName='0.1.6'" in badging,
            'Incorrect package or version')
    require("sdkVersion:'21'" in badging, 'Minimum Android version changed')
    require('leanback-launchable-activity:' in badging, 'Missing Android TV launcher')
    manifest = run(args.aapt, 'dump', 'xmltree', args.apk, 'AndroidManifest.xml')
    for name in ('android:icon', 'android:roundIcon', 'android:banner'):
        require(name in manifest, f'Missing {name}')
    table = run(args.aapt, 'dump', '--values', 'resources', args.apk)
    files = resource_files(table)

    def resolve(resource, config):
        matches = [path for (name, qualifiers), path in files.items()
                   if name == resource and re.fullmatch(config, qualifiers)]
        require(len(matches) == 1, f'Missing/ambiguous {resource} ({config})')
        return matches[0]

    resources = []
    with zipfile.ZipFile(args.apk) as apk:
        for density, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96),
                              ('xxhdpi', 144), ('xxxhdpi', 192)]:
            path = resolve('mipmap/app_icon', rf'{density}(?:-v\d+)?')
            resources.append((path, size, size, f'mipmap-{density}/app_icon.png'))
        icon = resolve('drawable/vela_youtube_store_icon', r'nodpi(?:-v\d+)?')
        banner = resolve('drawable/vela_youtube_tv_banner_nodpi', r'nodpi(?:-v\d+)?')
        require(re.search(r"^application:.* icon='" + re.escape(icon) +
                          r"' banner='" + re.escape(banner) + r"'", badging, re.M),
                'Launcher icon or TV banner does not resolve to the PNG resource')
        resources.append((icon, 512, 512, 'drawable-nodpi/vela_youtube_store_icon.png'))
        resources.append((banner, 320, 180, 'drawable-nodpi/vela_youtube_tv_banner_nodpi.png'))
        resources.append((resolve('drawable/vela_youtube_logo', r'\(default\)'),
                          1024, 1024, 'drawable/vela_youtube_logo.png'))
        for path, width, height, source_path in resources:
            data = apk.read(path)
            require(data[:8] == b'\x89PNG\r\n\x1a\n', f'Invalid PNG: {path}')
            require(struct.unpack('>II', data[16:24]) == (width, height),
                    f'Incorrect PNG dimensions: {path}')
            source = Path('app/src/main/res') / source_path
            with Image.open(io.BytesIO(data)) as compiled, Image.open(source) as original:
                require(compiled.convert('RGBA').tobytes() == original.convert('RGBA').tobytes(),
                        f'Compiled PNG pixels differ from supplied artwork: {path}')
        adaptive = resolve('mipmap/app_icon', 'anydpi-v26')
        xml = run(args.aapt, 'dump', 'xmltree', args.apk, adaptive)
        require('adaptive-icon' in xml and 'foreground' in xml, 'Invalid adaptive icon')
        require('monochrome' not in xml, 'Stale monochrome icon')
    report = dict(package='net.velalab.youtube', versionCode=7, versionName='0.1.6',
                  certificateSha256=cert_sha256, signatureSchemes=['v1', 'v2', 'v3'],
                  apkSha256=hashlib.sha256(Path(args.apk).read_bytes()).hexdigest(),
                  pngResources=[dict(path=p, width=w, height=h, source=s, pixelsMatch=True)
                                for p, w, h, s in resources],
                  androidTvLauncher=True, adaptiveIcon=True, roundIcon=True,
                  signatureOutput=signature)
    Path(args.report).write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
