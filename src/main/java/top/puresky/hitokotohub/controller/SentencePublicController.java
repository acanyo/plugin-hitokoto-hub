package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.index.query.Queries;
import run.halo.app.plugin.ApiVersion;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

@Slf4j
@ApiVersion("api.hitokotohub.puresky.top/v1alpha1")
@RequestMapping("/sentence")
@RestController
@RequiredArgsConstructor
@Tag(name = "SentencePublicV1alpha1")
public class SentencePublicController {

    private final ReactiveExtensionClient client;

    private final Map<String, Long> likeCache = new ConcurrentHashMap<>();
    private static final long LIKE_COOLDOWN = Duration.ofHours(12).toMillis();

    @GetMapping("/random")
    @Operation(summary = "随机获取句子")
    public Mono<RandomSentenceResponse> getRandomSentences(
        @Parameter(description = "分类名称，不传则返回所有类型")
        @RequestParam(name = "categoryName", required = false) String categoryName,
        @Parameter(description = "返回数量，默认8条，最多20条")
        @RequestParam(name = "limit", defaultValue = "8") int limit) {

        int actualLimit = Math.min(limit, 20);

        ListOptions options;
        if (categoryName != null && !categoryName.isBlank()) {
            options = ListOptions.builder().fieldQuery(
                Queries.and(Queries.equal("spec.categoryName", categoryName),
                    Queries.equal("status.isPublished", true),
                    Queries.isNull("metadata.deletionTimestamp"))).build();
        } else {
            options = ListOptions.builder().fieldQuery(
                Queries.and(Queries.equal("status.isPublished", true),
                    Queries.isNull("metadata.deletionTimestamp"))).build();
        }

        Mono<String> displayNameMono;
        if (categoryName != null && !categoryName.isBlank()) {
            displayNameMono = client.get(Category.class, categoryName)
                .map(category -> category.getSpec().getName()).defaultIfEmpty(categoryName);
        } else {
            displayNameMono = Mono.just("全部");
        }

        return client.countBy(Sentence.class, options).filter(total -> total > 0).flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / actualLimit);
            int randomPage = RandomUtils.insecure().randomInt(1, totalPages + 1);

            return client.listBy(Sentence.class, options,
                    PageRequestImpl.of(randomPage, actualLimit, Sort.unsorted()))
                .map(ListResult::getItems);
        }).defaultIfEmpty(Collections.emptyList()).zipWith(displayNameMono).map(tuple -> {
            List<Sentence> sentences = tuple.getT1();
            String displayName = tuple.getT2();

            Collections.shuffle(sentences);

            List<SentenceItem> items = sentences.stream().map(s -> {
                SentenceItem item = new SentenceItem();
                item.setAuthor(s.getSpec().getAuthor());
                item.setContent(s.getSpec().getContent());
                item.setSource(s.getSpec().getSource());
                item.setCreatedBy(s.getSpec().getCreatedBy());
                item.setLikeCount(s.getStatus() != null ? s.getStatus().getLikeCount() : 0);
                item.setViewCount(s.getStatus() != null ? s.getStatus().getViewCount() : 0);
                return item;
            }).toList();

            RandomSentenceResponse response = new RandomSentenceResponse();
            response.setCategoryName(displayName);
            response.setRequested(actualLimit);
            response.setReturned(items.size());
            response.setSentences(items);
            return response;
        });
    }

    @GetMapping("/like")
    @Operation(summary = "点赞/取消点赞句子")
    public Mono<LikeResponse> toggleLike(
        @RequestParam(name = "name")
        @Parameter(description = "句子名称")
        String name,
        @Parameter(description = "操作类型，like 或 unlike")
        @RequestParam(name = "action", defaultValue = "like")
        String action,
        ServerHttpRequest request) {

        String ip = getClientIp(request);
        String likeKey = ip + ":like:" + name;
        String unlikeKey = ip + ":unlike:" + name;
        boolean isUnlike = "unlike".equals(action);

        // 检查冷却
        String checkKey = isUnlike ? unlikeKey : likeKey;
        Long lastTime = likeCache.get(checkKey);
        long now = System.currentTimeMillis();

        if (lastTime != null && (now - lastTime) < LIKE_COOLDOWN) {
            long remainingSeconds = 12 * 60 * 60 - ((now - lastTime) / 1000);
            return client.get(Sentence.class, name)
                .map(sentence -> buildLikeResponse(sentence, false,
                    "请在 " + formatRemainingTime(remainingSeconds) + " 后再" + (isUnlike ? "取消点赞" : "点赞"),
                    "rate_limited"))
                .defaultIfEmpty(buildErrorResponse());
        }

        // 执行操作
        String oppositeKey = isUnlike ? likeKey : unlikeKey;

        return client.get(Sentence.class, name)
            .flatMap(sentence -> {
                if (sentence.getStatus() == null) {
                    sentence.setStatus(new Sentence.Status());
                }

                long currentLikes = sentence.getStatus().getLikeCount();

                if (isUnlike) {
                    sentence.getStatus().setLikeCount(Math.max(0, currentLikes - 1));
                } else {
                    sentence.getStatus().setLikeCount(currentLikes + 1);
                }

                return client.update(sentence).map(updated -> {
                    likeCache.put(checkKey, System.currentTimeMillis());
                    likeCache.remove(oppositeKey);
                    return buildLikeResponse(updated, true,
                        isUnlike ? "取消点赞成功" : "点赞成功", "ok");
                });
            })
            .defaultIfEmpty(buildErrorResponse());
    }

    private LikeResponse buildLikeResponse(Sentence sentence, boolean success, String message,
        String code) {
        LikeResponse response = new LikeResponse();
        response.setSuccess(success);
        response.setMessage(message);
        response.setCode(code);
        response.setSentence(toSentenceItem(sentence));
        return response;
    }

    private LikeResponse buildErrorResponse() {
        LikeResponse response = new LikeResponse();
        response.setSuccess(false);
        response.setMessage("句子不存在");
        response.setCode("not_found");
        response.setSentence(null);
        return response;
    }

    private SentenceItem toSentenceItem(Sentence s) {
        SentenceItem item = new SentenceItem();
        item.setAuthor(s.getSpec().getAuthor());
        item.setContent(s.getSpec().getContent());
        item.setSource(s.getSpec().getSource());
        item.setCreatedBy(s.getSpec().getCreatedBy());
        item.setLikeCount(s.getStatus() != null ? s.getStatus().getLikeCount() : 0);
        item.setViewCount(s.getStatus() != null ? s.getStatus().getViewCount() : 0);
        return item;
    }

    private String formatRemainingTime(long seconds) {
        if (seconds < 60) return seconds + " 秒";
        if (seconds < 3600) return (seconds / 60) + " 分钟";
        return (seconds / 3600) + " 小时";
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    @Data
    public static class RandomSentenceResponse {
        @Schema(description = "请求的分类名称")
        private String categoryName;
        @Schema(description = "请求数量")
        private long requested;
        @Schema(description = "实际返回数量")
        private long returned;
        @Schema(description = "句子列表")
        private List<SentenceItem> sentences;
    }

    @Data
    public static class SentenceItem {
        @Schema(description = "作者")
        private String author;
        @Schema(description = "内容")
        private String content;
        @Schema(description = "来源")
        private String source;
        @Schema(description = "创建者")
        private String createdBy;
        @Schema(description = "点赞数")
        private long likeCount;
        @Schema(description = "浏览数")
        private long viewCount;
    }

    @Data
    public static class LikeResponse {
        @Schema(description = "是否成功")
        private boolean success;
        @Schema(description = "状态码：ok / rate_limited / not_found")
        private String code;
        @Schema(description = "提示信息")
        private String message;
        @Schema(description = "句子完整信息")
        private SentenceItem sentence;
    }
}