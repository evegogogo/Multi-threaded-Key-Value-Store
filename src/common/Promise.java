package common;

import common.Request;

import java.io.Serializable;

public class Promise implements Serializable {
    private String serverID;
    private long proposalNum;
    private long prevProposalNum;
    private Request prevRequest;

    public String getServerID() {
        return serverID;
    }

    public void setServerID(String serverID) {
        this.serverID = serverID;
    }

    public long getPrevProposalNum() {
        return prevProposalNum;
    }

    public void setPrevProposalNum(long prevProposalNum) {
        this.prevProposalNum = prevProposalNum;
    }

    public long getProposalNum() {
        return proposalNum;
    }

    public void setProposalNum(long proposalNum) {
        this.proposalNum = proposalNum;
    }

    public Request getPrevAcceptedValue() {
        return prevRequest;
    }

    public void setPrevAcceptedValue(Request prevRequest) {
        this.prevRequest = prevRequest;
    }
}
