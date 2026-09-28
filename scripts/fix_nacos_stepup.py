#!/usr/bin/env python3
"""Fix duplicate stepup in Nacos common.yml - merge into single section."""
import urllib.request, base64, urllib.parse

auth = base64.b64encode(b'nacos:nacos').decode()

# Read existing
req = urllib.request.Request('http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=DEFAULT_GROUP')
req.add_header('Authorization', 'Basic ' + auth)
resp = urllib.request.urlopen(req)
content = resp.read().decode('utf-8')

# Find the first stepup line
lines = content.split('\n')
first_stepup = None
second_stepup = None
for i, line in enumerate(lines):
    if line.strip().startswith('stepup:'):
        if first_stepup is None:
            first_stepup = i
        elif second_stepup is None:
            second_stepup = i

print('First stepup at line:', first_stepup + 1 if first_stepup else None)
print('Second stepup at line:', second_stepup + 1 if second_stepup else None)

if second_stepup is not None:
    # Keep everything up to the comment before second stepup (inclusive)
    # Find the comment lines before the second stepup
    cut_line = second_stepup - 1
    # Walk backwards to find blank lines to trim
    while cut_line > 0 and lines[cut_line].strip() == '':
        cut_line -= 1
    # Walk back to the comment header
    while cut_line > 0 and lines[cut_line].strip().startswith('#'):
        cut_line -= 1
    # Now cut_line is at the last line to keep
    # Trim trailing blank lines
    while cut_line > 0 and lines[cut_line].strip() == '':
        cut_line -= 1
    
    new_lines = lines[:cut_line + 1]
    new_content = '\n'.join(new_lines) + '\n'
    print('Old length:', len(content), 'bytes')
    print('New length:', len(new_content), 'bytes')
    print('Removed', len(lines) - len(new_lines), 'lines')
    
    # Push
    data = urllib.parse.urlencode({
        'dataId': 'common.yml',
        'group': 'DEFAULT_GROUP',
        'type': 'yaml',
        'content': new_content
    }).encode()
    
    req2 = urllib.request.Request(
        'http://localhost:8848/nacos/v1/cs/configs',
        data=data,
        method='POST'
    )
    req2.add_header('Authorization', 'Basic ' + auth)
    req2.add_header('Content-Type', 'application/x-www-form-urlencoded')
    resp2 = urllib.request.urlopen(req2)
    print('Publish result:', resp2.read().decode())
    
    # Verify
    req3 = urllib.request.Request('http://localhost:8848/nacos/v1/cs/configs?dataId=common.yml&group=DEFAULT_GROUP')
    req3.add_header('Authorization', 'Basic ' + auth)
    resp3 = urllib.request.urlopen(req3)
    updated = resp3.read().decode()
    c = updated.count('stepup:')
    print('stepup count after fix:', c)
    if c == 1:
        print('FIXED!')
    else:
        print('Still broken!')
else:
    print('No duplicate found, no fix needed')