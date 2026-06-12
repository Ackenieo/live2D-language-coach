package com.ackenieo.init_pro.domain.model.entity.base;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 领域实体基类（MyBatis-Plus）
 */
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    protected String id;
    
    @TableField(fill = FieldFill.INSERT)
    protected LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime updatedAt;
    
    @TableLogic
    protected Integer deleted;

    public BaseEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
