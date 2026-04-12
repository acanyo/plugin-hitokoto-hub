package top.puresky.hitokotohub.reconciler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import top.puresky.hitokotohub.extension.Sentence;

@Component
@RequiredArgsConstructor
public class SentenceReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    @Override
    public Result reconcile(Request request) {
        // ...
        return null;
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new Sentence())
            .build();
    }
}