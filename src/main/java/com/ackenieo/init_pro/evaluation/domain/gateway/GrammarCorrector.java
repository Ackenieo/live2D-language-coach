package com.ackenieo.init_pro.evaluation.domain.gateway;

/**
 * 语法纠错接口
 */
public interface GrammarCorrector {
    String correct(String text);
    boolean isEnabled();
}
