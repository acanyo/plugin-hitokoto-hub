package top.puresky.hitokotohub.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(
    group = "hitokotohub.puresky.top",
    version = "v1alpha1",
    kind = "Sentence",
    plural = "sentences",
    singular = "sentence"
)
public class Sentence extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    private Status status;

    @Data
    @Schema(name = "SentenceSpec")
    public static class Spec {
        @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String categoryName;

        @Schema(description = "句子内容", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
        private String content;

        @Schema(description = "作者", maxLength = 50)
        private String author;

        @Schema(description = "来源", maxLength = 100)
        private String source;
    }

    @Data
    @Schema(name = "SentenceStatus")
    public static class Status {
        @Schema(description = "点赞数")
        private Integer likeCount;

        @Schema(description = "浏览量")
        private Integer viewCount;

    }
}