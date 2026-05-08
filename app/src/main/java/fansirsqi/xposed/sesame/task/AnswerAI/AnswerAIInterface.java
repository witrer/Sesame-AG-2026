package fansirsqi.xposed.sesame.task.AnswerAI;

import java.util.List;

/**
 * AI答题服务接口
 * 定义了AI答题服务的基本操作，包括获取答案、设置模型等功能
 */
public interface AnswerAIInterface {

    /**
     * 设置模型名称
     *
     * @param modelName 模型名称
     */
    default void setModelName(String modelName) {
        // 默认空实现
    }

    /**
     * 获取模型名称
     *
     * @return 当前使用的模型名称
     */
    default String getModelName() {
        // 默认空实现
        return "";
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果，如果获取失败返回空字符串
     */
    String getAnswerStr(String text);

    /**
     * 获取AI回答结果，指定模型
     *
     * @param text  问题内容
     * @param model 模型名称
     * @return AI回答结果，如果获取失败返回空字符串
     */
    String getAnswerStr(String text, String model);

    /**
     * 获取AI答案
     *
     * @param title      问题标题
     * @param answerList 候选答案列表
     * @return 选中的答案索引，如果没有找到合适的答案返回-1
     */
    Integer getAnswer(String title, List<String> answerList);

    /**
     * 释放资源
     * 实现类应在此方法中清理所有使用的资源
     */
    default void release() {
        // 默认空实现
    }

    /**
     * 获取单例实例
     *
     * @return 默认的AI答题服务实现
     */
    static AnswerAIInterface getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 单例持有者，延迟加载
     */
    class SingletonHolder {
        private static final AnswerAIInterface INSTANCE = new AnswerAIInterface() {
            @Override
            public String getAnswerStr(String text) {
                return "";
            }

            @Override
            public String getAnswerStr(String text, String model) {
                return "";
            }

            @Override
            public Integer getAnswer(String title, List<String> answerList) {
                return -1;
            }
        };
    }
}