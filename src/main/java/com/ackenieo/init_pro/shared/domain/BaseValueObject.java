package com.ackenieo.init_pro.shared.domain;

import java.io.Serializable;

/**
 * 值对象基类
 */
public abstract class BaseValueObject implements Serializable {
    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
