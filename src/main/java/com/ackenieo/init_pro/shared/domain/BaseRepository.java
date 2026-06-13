package com.ackenieo.init_pro.shared.domain;

import java.util.List;
import java.util.Optional;

/**
 * 仓储基类接口
 */
public interface BaseRepository<T extends BaseEntity, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
    boolean existsById(ID id);
}
