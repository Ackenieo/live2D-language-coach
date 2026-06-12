package com.ackenieo.init_pro.application.command;

/**
 * 命令基类
 */
public abstract class BaseCommand {
    private final String commandId;

    protected BaseCommand() {
        this.commandId = java.util.UUID.randomUUID().toString();
    }

    public String getCommandId() {
        return commandId;
    }
}
