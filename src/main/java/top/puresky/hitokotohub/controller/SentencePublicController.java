package top.puresky.hitokotohub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
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
                    Queries.equal("status.isPublished", true))).build();
        } else {
            options = ListOptions.builder().build();
        }

        // 查询分类的中文名称
        Mono<String> displayNameMono;
        if (categoryName != null && !categoryName.isBlank()) {
            displayNameMono = client.get(Category.class, categoryName)
                .map(category -> category.getSpec().getName()).defaultIfEmpty(categoryName);
        } else {
            displayNameMono = Mono.just("全部");
        }

        // 并发查询句子和分类名
        return Mono.zip(client.listAll(Sentence.class, options, Sort.unsorted())
            .filter(sentence -> sentence.getMetadata().getDeletionTimestamp() == null)
            .filter(sentence -> sentence.getStatus() != null && sentence.getStatus().isPublished())
            .collectList(), displayNameMono).map(tuple -> {
            List<Sentence> sentences = tuple.getT1();
            String displayName = tuple.getT2();

            // 随机打乱
            Collections.shuffle(sentences);
            List<Sentence> result = sentences.stream().limit(actualLimit).toList();

            // 转换成 SentenceItem
            List<SentenceItem> items = result.stream().map(s -> {
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
}