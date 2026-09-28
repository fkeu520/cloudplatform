#!/usr/bin/env python3
"""Count stepup occurrences in Nacos common.yml."""
import urllib.request, base64

auth = base64.b64encode(b'nacos:nacos').decode()
req = urllib.request.Request('http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=DEFAULT_GROUP')
req.add_header('Authorization', 'Basic ' + auth)
resp = urllib.request.urlopen(req)
content = resp.read().decode('utf-8')

count = 0
for i, line in enumerate(content.split('\n'), 1):
    if 'stepup' in line.lower():
        print('Line %d: %s' % (i, line.strip()))
        count += 1

print('Total stepup: count:', count)
print('Total lines:', len(content.split('\n')))