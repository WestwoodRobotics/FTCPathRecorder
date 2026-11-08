package org.westwoodrobots.pathrecorder;

public interface CommandRuntime<C> {
    void schedule(C command);

    void execute();

    CodeGenBackend backend();
}
