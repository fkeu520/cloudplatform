#!/usr/bin/env python3
"""Check stepup key in Nacos common.yml."""
import urllib.request, base64

auth = base64.b64encode(b'nacos:nacos').decode()
req = urllib.request.Request('http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=DEFAULT_GROUP')
req.add_header('Authorization', 'Basic ' + auth)
resp = urllib.request.urlopen(req)
content = resp.read().decode('utf-8')

count = content.count('stepup:')
print('stepup: count:', count)

for i, line in enumerate(content.split('\n'), 1):
    ls = line.strip()
    if 'stepup' in ls.lower():
        print('Line %d: %s' % (i, ls))

# Check if stepup also in application.yml of auth
print('\nChecking if application.yml has stepup...')
try:
    with open('/opt/platform/code/platform-server/platform-auth/src/main/resources/application.yml') as f:
        app_yml = f.read()
    app_count = app_yml.count('stepup:')
    print('auth application.yml stepup count:', app_count)
    if app_count > 0:
        for i, line in enumerate(app_yml.split('\n'), 1):
            if 'stepup' in line.lower():
                print('  Line %d: %s' % (i, line.strip()))
except FileNotFoundError:
    print('File not found (running on server, not from git?)')