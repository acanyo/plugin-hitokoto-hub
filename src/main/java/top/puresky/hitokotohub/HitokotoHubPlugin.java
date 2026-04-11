package top.puresky.hitokotohub;

import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import top.puresky.hitokotohub.extension.Category;
import top.puresky.hitokotohub.extension.Sentence;

/**
 * 一言库插件入口
 *
 * @author Cyon
 * @since 1.0.0
 */
@Component
public class HitokotoHubPlugin extends BasePlugin {

    private final SchemeManager schemeManager;

    public HitokotoHubPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        // 注册自定义模型
        schemeManager.register(Sentence.class);
        schemeManager.register(Category.class);
        System.out.println("✅ 一言数据中心插件启动成功！");
    }

    @Override
    public void stop() {
        // 插件停用时取消注册自定义模型
        Scheme sentenceScheme = schemeManager.get(Sentence.class);
        Scheme categoryScheme = schemeManager.get(Category.class);
        schemeManager.unregister(sentenceScheme);
        schemeManager.unregister(categoryScheme);
        System.out.println("✅ 一言数据中心插件已停止！");
    }
}