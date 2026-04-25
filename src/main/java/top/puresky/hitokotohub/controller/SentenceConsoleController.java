package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ApiVersion;
import top.puresky.hitokotohub.extension.Sentence;

@ApiVersion("console.api.hitokotohub.puresky.top/v1alpha1")
@RequestMapping("/sentence")
@RestController
@RequiredArgsConstructor
@Tag(name = "SentenceV1alpha1")
public class SentenceConsoleController {

    private final ReactiveExtensionClient client;
    private final RoleService roleService;

    @PostMapping("/batch")
    @Operation(summary = "批量创建句子")
    public Mono<BatchCreateSentenceResult> batchCreateSentence(
        @RequestBody Flux<Sentence> sentenceFlux, @AuthenticationPrincipal UserDetails user) {
        String username = user.getUsername();
        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        return roleService.getRolesByUsername(username).collectList().flatMapMany(roles -> {
            boolean hasSuperRole = roles.contains("super-role");

            return sentenceFlux.flatMap(sentence -> {
                sentence.getSpec().setCreatedBy(username);
                // 初始化 status 如果为空
                if (sentence.getStatus() == null) {
                    sentence.setStatus(new Sentence.Status());
                }
                sentence.getStatus().setPublished(hasSuperRole);
                return client.create(sentence).doOnSuccess(s -> {
                    success.incrementAndGet();
                    total.incrementAndGet();
                }).onErrorResume(e -> {
                    failed.incrementAndGet();
                    total.incrementAndGet();
                    return Mono.empty();
                });
            });
        }).then(Mono.fromCallable(() -> {
            BatchCreateSentenceResult result = new BatchCreateSentenceResult();
            result.setTotal(total.get());
            result.setSuccess(success.get());
            result.setFailed(failed.get());
            return result;
        }));
    }

    @Data
    public static class BatchCreateSentenceResult {
        private long total;
        private long success;
        private long failed;
    }
}