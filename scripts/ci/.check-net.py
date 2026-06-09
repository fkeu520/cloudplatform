import json, sys
with open('scripts/ci/.logstash-inspect.json', 'r', encoding='utf-8') as f:
    raw = f.read()
# Skip rtk banner
start = raw.find('[{')
if start > 0:
    raw = raw[start:]
data = json.loads(raw)
networks = data[0]['NetworkSettings']['Networks']
print('logstash networks:')
for name, net in networks.items():
    print('  {}: IP={}'.format(name, net.get('IPAddress')))
