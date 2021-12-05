package common;

import common.Request;

import java.io.Serializable;

public class Accept implements Serializable {
    private String serverID;
    private long proposalNum;
    private Request request;

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public long getProposalNum() {
        return proposalNum;
    }

    public void setProposalNum(long proposalNum) {
        this.proposalNum = proposalNum;
    }

    public Request getValue() {
        return request;
    }

    public void setValue(Request request) {
        this.request = request;
    }
}
