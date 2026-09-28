#!/usr/bin/env python3
"""Show Nacos common.yml around stepup lines."""
import urllib.request, base64

auth = base64.b64encode(b'nacos:nacos').decode()
req = urllib.request.Request('http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=DEFAULT_GROUP')
req.add_header('Authorization', 'Basic ' + auth)
resp = urllib.request.urlopen(req)
content = resp.read().decode('utf-8')

lines = content.split('\n')
print('Lines 70-88:')
for i in range(69, min(88, len(lines))):
    print('  %d: %s' % (i+1, lines[i]))