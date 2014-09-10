package io.myweb.http;


import java.io.FileDescriptor;

public interface ResponseCallback {
    public void writeBody(FileDescriptor fd);
}
