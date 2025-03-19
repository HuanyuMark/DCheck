package org.example.dcheck.api;

/**
 * Date: 2025/3/17
 * 在查重正式开始前，需要先借助该实例，过滤一遍所要查重的文档，命中或者与白名单中关键字相似的片段不参与查重
 * rule set storage
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public interface WhiteListManager {

    WhiteListRuleSet getRuleSet(String whiteListId);

    void removeRuleSet(String whiteListId);

}
