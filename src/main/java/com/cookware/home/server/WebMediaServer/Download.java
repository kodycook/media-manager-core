package com.cookware.home.server.WebMediaServer;

/**
 * Created by Kody on 21/08/2017.
 */
public class Download {
    private String name;
    private DownloadState state;
    private String url;
    private int priority;

    public Download(String mName, String mUrl, int mPriority){
        this.name = mName;
        this.priority = mPriority;
        this.url = mUrl;
        this.state = DownloadState.QUEUED;
    }

    public int getPriority(){
        return this.priority;
    }

    public DownloadState getState(){
        return this.state;
    }

    public void setState(DownloadState newState){
        this.state = newState;
    }

    public String toString(){
        return String.format("%s <%d>: \t%s - %s", this.name, this.priority, this.state, this.url);
    }
}
