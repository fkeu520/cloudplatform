#!/usr/bin/env python3
"""Debug: fetch and print public key response."""
import urllib.request, json
resp = urllib.request.urlopen('http://192.168.0.142:8083/auth/public-key')
data = json.loads(resp.read().decode())
print('Type:', type(data))
print('Keys:', list(data.keys()))
print('Full:', json.dumps(data, ensure_ascii=False, indent=2)[:500])