package com.github.davidmoten.resources.analyzer;

import com.github.davidmoten.resources.analyzer.execution.DownloadQueued;
import com.github.davidmoten.resources.analyzer.execution.Execution;
import com.github.davidmoten.resources.analyzer.execution.DownloadSucceeded;
import com.github.davidmoten.resources.analyzer.execution.DownloadTimeout;
import com.github.davidmoten.resources.analyzer.execution.Downloading;
import com.github.davidmoten.resources.analyzer.execution.Started;
import com.github.davidmoten.resources.analyzer.execution.Failed;
import com.github.davidmoten.resources.analyzer.execution.Succeeded;
import com.github.davidmoten.resources.analyzer.execution.InProgress;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachineDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class StateMachineDefinitions implements Supplier<List<StateMachineDefinition<?>>> {
    public List<StateMachineDefinition<?>> get() {
        return Arrays.asList(
                createVinDiscoveryStateMachine());
    }

    public static StateMachineDefinition<Execution> createVinDiscoveryStateMachine() {
        StateMachineDefinition<Execution> m  = StateMachineDefinition.create(Execution.class);

        State<Execution, Started> started =
                m.createState("STARTED", Started.class);

        State<Execution, DownloadQueued> downloadQueued =
            m.createState("DOWNLOAD_QUEUED", DownloadQueued.class);

        State<Execution, Downloading> downloading =
            m.createState("DOWNLOADING", Downloading.class);

        State<Execution, DownloadSucceeded> downloadSucceeded =
            m.createState("DOWNLOAD_SUCCEEDED", DownloadSucceeded.class);

        State<Execution, DownloadTimeout> downloadTimeout =
            m.createState("DOWNLOAD_TIMEOUT", DownloadTimeout.class);

        State<Execution, InProgress> inProgress =
                m.createState("IN_PROGRESS", InProgress.class);

        State<Execution, Failed> failed =
                m.createState("FAILED", Failed.class);
        State<Execution, Succeeded> succeeded =
                m.createState("SUCCEEDED", Succeeded.class);

        started.to(inProgress)
            .to(downloadQueued)
            .to(downloading)
            .to(downloadSucceeded)
            .to(inProgress)
            .to(succeeded);

        downloading.to(downloadTimeout);

        downloadTimeout.to(downloadQueued);

        inProgress.to(failed);

        return m;
    }
}
