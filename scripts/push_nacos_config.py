#!/usr/bin/env python3
"""Push common.yml with stepup section to Nacos."""
import urllib.request, base64, urllib.parse, sys

auth = base64.b64encode(b'nacos:nacos').decode()
base_url = 'http://localhost:8848/nacos/v1/cs/configs'

def get_config():
    req = urllib.request.Request(base_url + '?dataId=common.yml&group=DEFAULT_GROUP')
    req.add_header('Authorization', 'Basic ' + auth)
    resp = urllib.request.urlopen(req)
    return resp.read().decode('utf-8')

def publish_config(content):
    data = urllib.parse.urlencode({
        'dataId': 'common.yml',
        'group': 'DEFAULT_GROUP',
        'type': 'yaml',
        'content': content
    }).encode()
    req = urllib.request.Request(base_url, data=data, method='POST')
    req.add_header('Authorization', 'Basic ' + auth)
    req.add_header('Content-Type', 'application/x-www-form-urlencoded')
    resp = urllib.request.urlopen(req)
    return resp.read().decode()

# Read existing
existing = get_config()
sys.stdout.write('Existing config length: %d bytes\n' % len(existing))
sys.stdout.write('Ends with INFO: %s\n' % str(existing.rstrip().endswith('INFO')))

# Append stepup
existing_stripped = existing.rstrip()
stepup_block = '\n\n'
stepup_block += '# ============ 7. v8 P0-3 Step-up (高敏操作二次鉴权) ============\n'
stepup_block += '# ADR-008: doc/decision/v8-P0-3-step-up-token.md\n'
stepup_block += 'stepup:\n'
stepup_block += '  ttl-seconds: 300\n'
stepup_block += '  max-issues-per-window: 3\n'
stepup_block += '  rate-limit-seconds: 300\n'

new_content = existing_stripped + stepup_block
sys.stdout.write('New config length: %d bytes\n' % len(new_content))

# Publish
result = publish_config(new_content)
sys.stdout.write('Publish result: [%s]\n' % result)

# Verify
if result == 'true':
    updated = get_config()
    has_stepup = 'stepup:' in updated
    sys.stdout.write('Has stepup: %s\n' % str(has_stepup))
    sys.stdout.write('Updated config length: %d bytes\n' % len(updated))
    if has_stepup:
        lines = updated.strip().split('\n')
        for l in lines[-6:]:
            sys.stdout.write('  ' + l + '\n')
    else:
        lines = updated.strip().split('\n')
        sys.stdout.write('Last 10 lines:\n')
        for l in lines[-10:]:
            sys.stdout.write('  ' + l + '\n')
else:
    sys.stdout.write('Publish FAILED!\n')
    sys.exit(1)
