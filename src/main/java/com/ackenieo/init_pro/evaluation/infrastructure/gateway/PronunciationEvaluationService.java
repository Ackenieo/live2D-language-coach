package com.ackenieo.init_pro.evaluation.infrastructure.gateway;

import com.ackenieo.init_pro.evaluation.domain.entity.PronunciationResult;
import com.ackenieo.init_pro.evaluation.domain.gateway.PronunciationEvaluator;
import com.ackenieo.init_pro.evaluation.infrastructure.config.EvaluationConfig;
import com.tencent.core.ws.Credential;
import com.tencent.core.ws.SpeechClient;
import com.tencent.soe.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 发音评测服务
 */
@Service
public class PronunciationEvaluationService implements PronunciationEvaluator {
    private static final Logger log = LoggerFactory.getLogger(PronunciationEvaluationService.class);

    private final boolean enabled;
    private final String appId;
    private final String secretId;
    private final String secretKey;

    public PronunciationEvaluationService(EvaluationConfig config) {
        this.enabled = config.isTencentSoeEnabled();
        this.appId = config.getTencentAppId();
        this.secretId = config.getTencentSecretId();
        this.secretKey = config.getTencentSecretKey();

        if (this.enabled && !this.appId.isEmpty() && !this.secretId.isEmpty() && !this.secretKey.isEmpty()) {
            log.info("腾讯智聆发音测评服务已启用");
        } else {
            log.info("腾讯智聆发音测评服务未启用或配置不完整");
        }
    }

    @Override
    @Async
    public CompletableFuture<PronunciationResult> evaluate(String turnId, String sessionId, byte[] audioData, String refText) {
        return CompletableFuture.completedFuture(doEvaluate(turnId, sessionId, audioData, refText));
    }

    private PronunciationResult doEvaluate(String turnId, String sessionId, byte[] audioData, String refText) {
        if (!enabled || audioData == null || audioData.length == 0 || refText == null || refText.trim().isEmpty()) {
            return null;
        }

        SpeechClient client = null;
        OralEvaluator evaluator = null;

        try {
            AtomicReference<PronunciationResult> resultRef = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            Credential credential = new Credential();
            credential.setAppid(appId);
            credential.setSecretId(secretId);
            credential.setSecretKey(secretKey);

            client = new SpeechClient(OralEvalConstant.DEFAULT_ORAL_EVAL_REQ_URL);

            OralEvaluationRequest request = new OralEvaluationRequest();
            request.setServerEngineType("16k_zh");
            request.setVoiceFormat(0);
            request.setTextMode(0);
            request.setRefText(refText);
            request.setEvalMode(1);
            request.setScoreCoeff(1.0);
            request.setSentenceInfoEnabled(1);
            request.setRecMode(1);

            OralEvaluationListener listener = new OralEvaluationListener() {
                @Override
                public void OnIntermediateResults(OralEvaluationResponse response) {
                    log.debug("发音测评中间结果: code={}", response.getCode());
                }

                @Override
                public void onRecognitionStart(OralEvaluationResponse response) {
                    log.info("发音测评开始, turnId={}", turnId);
                }

                @Override
                public void onRecognitionComplete(OralEvaluationResponse response) {
                    if (response.getResult() != null) {
                        SentenceInfo result = response.getResult();
                        resultRef.set(new PronunciationResult(
                                turnId,
                                sessionId,
                                refText,
                                result.getSuggestedScore(),
                                result.getPronAccuracy(),
                                result.getPronFluency(),
                                result.getPronCompletion(),
                                PronunciationResult.fromScore(result.getSuggestedScore()),
                                PronunciationResult.fromScore(result.getPronAccuracy()),
                                PronunciationResult.fromScore(result.getPronFluency()),
                                PronunciationResult.fromScore(result.getPronCompletion())
                        ));
                        log.info("发音测评完成, turnId={}, score={}", turnId, result.getSuggestedScore());
                    }
                    latch.countDown();
                }

                @Override
                public void onFail(OralEvaluationResponse response) {
                    log.error("发音测评失败, turnId={}, code={}, message={}", turnId, response.getCode(), response.getMessage());
                    latch.countDown();
                }

                @Override
                public void onMessage(OralEvaluationResponse response) {
                    log.debug("发音测评消息: code={}, end={}", response.getCode(), response.getEnd());
                }
            };

            evaluator = new OralEvaluator(client, credential, request, listener);
            evaluator.start(15000);
            evaluator.write(audioData);
            evaluator.stop(15000);

            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("发音测评超时, turnId={}", turnId);
                return null;
            }

            return resultRef.get();
        } catch (Exception e) {
            log.error("发音测评异常, turnId={}: {}", turnId, e.getMessage(), e);
            return null;
        } finally {
            if (evaluator != null) {
                try {
                    evaluator.close();
                } catch (Exception e) {
                    log.debug("关闭evaluator失败", e);
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
