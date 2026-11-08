package org.westwoodrobots.pathrecorder.ivy;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import org.westwoodrobots.pathrecorder.CodeGenBackend;
import org.westwoodrobots.pathrecorder.CommandRuntime;

public final class IvyRuntime implements CommandRuntime<Command> {
    private final boolean runScheduler;
    private final CodeGenBackend backend = new IvyBackend();

    private IvyRuntime(boolean runScheduler) {
        this.runScheduler = runScheduler;
    }

    // the recorder calls Scheduler.execute() once per update
    public static IvyRuntime owned() {
        return new IvyRuntime(true);
    }

    // team code already calls Scheduler.execute() each loop
    public static IvyRuntime external() {
        return new IvyRuntime(false);
    }

    @Override
    public void schedule(Command command) {
        command.schedule();
    }

    @Override
    public void execute() {
        if (runScheduler) Scheduler.execute();
    }

    @Override
    public CodeGenBackend backend() {
        return backend;
    }
}
