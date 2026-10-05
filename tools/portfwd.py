import socket, threading, sys

HOST, PORT = ("127.0.0.1", 15005)
TARGET = ("192.168.1.100", 5005)

def pipe(a, b):
    try:
        while True:
            d = a.recv(65536)
            if not d:
                break
            b.sendall(d)
    except Exception:
        pass
    finally:
        for s in (a, b):
            try:
                s.close()
            except Exception:
                pass

s = socket.socket()
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind((HOST, PORT))
s.listen(64)
print(f"forwarding {HOST}:{PORT} -> {TARGET}", flush=True)
while True:
    c, _ = s.accept()
    try:
        r = socket.create_connection(TARGET, timeout=8)
    except Exception as e:
        c.close()
        continue
    threading.Thread(target=pipe, args=(c, r), daemon=True).start()
    threading.Thread(target=pipe, args=(r, c), daemon=True).start()
