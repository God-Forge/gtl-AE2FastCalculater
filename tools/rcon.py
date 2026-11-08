import socket
import struct
import sys


def rcon_command(host, port, password, command):
    s = socket.create_connection((host, port), timeout=10)

    def send(req_id, req_type, payload):
        data = payload.encode() + b"\x00\x00"
        packet = struct.pack("<ii", req_id, req_type) + data
        s.sendall(struct.pack("<i", len(packet)) + packet)
        # read response
        length = struct.unpack("<i", s.recv(4))[0]
        buf = b""
        while len(buf) < length:
            chunk = s.recv(length - len(buf))
            if not chunk:
                break
            buf += chunk
        req_id_resp, req_type_resp = struct.unpack("<ii", buf[:8])
        return buf[8:-2].decode("utf-8", "replace")

    send(1, 3, password)
    result = send(2, 2, command)
    s.close()
    return result


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "list"
    print(rcon_command("127.0.0.1", 25575, "ecocalcerrcon", cmd))
