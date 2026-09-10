import hashlib, json, pathlib, re, subprocess, sys
serial, target = sys.argv[1:]
root = '/sdcard/Download/whip-ui-catalog'
paths = subprocess.run(['adb', '-s', serial, 'shell', 'ls', '-1t', root], capture_output=True, text=True).stdout.splitlines()
if not any(p.startswith('tracks.collection.') for p in paths):
    root = '/sdcard/Android/data/commvne.com.whip.app.debug/files/whip-ui-catalog'
    paths = subprocess.check_output(['adb', '-s', serial, 'shell', 'ls', '-1t', root], text=True).splitlines()
out = pathlib.Path(target)
out.mkdir(parents=True, exist_ok=True)
seen, records = set(), []
for name in paths:
    if not name.startswith('tracks.collection.') or not name.endswith(('.png', '.xml')):
        continue
    canonical = re.sub(r' \(\d+\)(?=\.(png|xml)$)', '', name)
    if canonical in seen: continue
    seen.add(canonical)
    destination = out / canonical
    subprocess.run(['adb', '-s', serial, 'pull', f'{root}/{name}', str(destination)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    records.append(dict(file=canonical, original=name, sha256=hashlib.sha256(destination.read_bytes()).hexdigest()))
(out / 'capture-manifest.json').write_text(json.dumps(records, indent=2) + '\n')
print(f'Pulled {len(records)} files to {out}')
