package ozudo.net;

import lombok.Data;

@Data
public class RequestMessage {
    private String magic;
    private int service;
    private byte[] body;
    private String mAddress = "127.0.0.1";


    public RequestMessage(String magic, int service, byte[] body, String mAddress) {
        this.magic = magic;
        this.service = service;
        this.body = body;
        this.mAddress = mAddress;
    }

    @Override
    public String toString() {
        return String.format("MAGIC:%s, SERVICE:%3d, BODY:%s", magic, service, body);
    }
}
