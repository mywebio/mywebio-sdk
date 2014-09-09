package io.myweb.api;


import java.io.FileDescriptor;

public interface ResponseCallback {
    public void writeBody(FileDescriptor fd);
}
