package me.jar.constants;

import me.jar.exception.TransferProxyException;

public enum TransferMsgType {
    REGISTER(1),
    REGISTER_RESULT(2),
    CONNECT(3),
    DISCONNECT(4),
    DATA(5),
    KEEPALIVE(6)
    ;

    int type;

    TransferMsgType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static TransferMsgType toTransferMsgType(int code) throws TransferProxyException {
        TransferMsgType[] values = TransferMsgType.values();
        for (TransferMsgType value : values) {
            if (value.type == code) {
                return value;
            }
        }
        throw new TransferProxyException("unavailable transfer message type code: " + code);
    }
}
