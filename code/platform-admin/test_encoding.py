import urllib.request

# Test auth endpoint - read raw bytes and print hex
url = "http://localhost:8080/auth/login"
data = b'{"username":"admin","password":"123456"}'
req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(req, timeout=5) as resp:
        raw = resp.read()
        print("HTTP status:", resp.status)
        print("Headers:", dict(resp.headers))
        print("Raw bytes (hex):", raw[:100].hex())
        print("As UTF-8:", raw.decode('utf-8'))
except Exception as e:
    print(f"Error: {e}")