package org.jsoup.exception;

public class ItemChangedException extends RuntimeException {

    private String retCd;
    private String msgDes;

    public ItemChangedException() {
        super();
    }

    public ItemChangedException(String message) {
        super(message);
        msgDes = message;
    }

    public ItemChangedException(String retCd, String msgDes) {
        super();
        this.retCd = retCd;
        this.msgDes = msgDes;
    }

    public String getRetCd() {
        return retCd;
    }

    public String getMsgDes() {
        return msgDes;
    }
}
