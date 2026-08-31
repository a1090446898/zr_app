package com.gsoft.inventory.entities;

public class BadgeNotificationPojo {

    /**
     * 待办数量
     */
    private String badgeCount;


    /**
     * 流程id
     */
    private String flowId;

    /**
     * 流程标识
     */
    private String flowTag;

    /**
     * 内容
     */
    private String noticeContent;


    public String getBadgeCount() {
        return badgeCount;
    }

    public void setBadgeCount(String badgeCount) {
        this.badgeCount = badgeCount;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowTag() {
        return flowTag;
    }

    public void setFlowTag(String flowTag) {
        this.flowTag = flowTag;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void setNoticeContent(String noticeContent) {
        this.noticeContent = noticeContent;
    }
}
