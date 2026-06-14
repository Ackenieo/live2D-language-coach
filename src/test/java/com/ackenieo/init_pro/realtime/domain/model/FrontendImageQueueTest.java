package com.ackenieo.init_pro.realtime.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendImageQueueTest {

    @Test
    void keepsOnlyLatestThreeImages() {
        FrontendImageQueue queue = new FrontendImageQueue(3);

        queue.enqueueAndTail("image-1", "prompt-1");
        queue.enqueueAndTail("image-2", "prompt-2");
        queue.enqueueAndTail("image-3", "prompt-3");
        FrontendImage tail = queue.enqueueAndTail("image-4", "prompt-4");

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.snapshot())
                .extracting(FrontendImage::base64Image)
                .containsExactly("image-2", "image-3", "image-4");
        assertThat(tail.base64Image()).isEqualTo("image-4");
        assertThat(tail.prompt()).isEqualTo("prompt-4");
        assertThat(queue.tail()).isEqualTo(tail);
    }

    @Test
    void returnsTailImageAfterEveryEnqueue() {
        FrontendImageQueue queue = new FrontendImageQueue(3);

        assertThat(queue.enqueueAndTail("image-1", "prompt-1").base64Image()).isEqualTo("image-1");
        assertThat(queue.enqueueAndTail("image-2", "prompt-2").base64Image()).isEqualTo("image-2");
        assertThat(queue.enqueueAndTail("image-3", "prompt-3").base64Image()).isEqualTo("image-3");
    }
}
