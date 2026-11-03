package com.shopledger.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Reminder {

    private Long id;
    private String title;
    private String content;
    private LocalDate dueDate;
    private String priority;
    private boolean done;
    private LocalDateTime createdAt;

    /** 与提醒标题匹配的本月支出分类（如“房租”），用于在提醒旁展示已记支出。 */
    private Long relatedCategoryId;
    private String relatedCategoryName;
    private BigDecimal relatedAmount;
    private LocalDate relatedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getRelatedCategoryId() {
        return relatedCategoryId;
    }

    public void setRelatedCategoryId(Long relatedCategoryId) {
        this.relatedCategoryId = relatedCategoryId;
    }

    public String getRelatedCategoryName() {
        return relatedCategoryName;
    }

    public void setRelatedCategoryName(String relatedCategoryName) {
        this.relatedCategoryName = relatedCategoryName;
    }

    public BigDecimal getRelatedAmount() {
        return relatedAmount;
    }

    public void setRelatedAmount(BigDecimal relatedAmount) {
        this.relatedAmount = relatedAmount;
    }

    public LocalDate getRelatedDate() {
        return relatedDate;
    }

    public void setRelatedDate(LocalDate relatedDate) {
        this.relatedDate = relatedDate;
    }

    public String getDueDateText() {
        return dueDate == null ? "" : dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public long getDaysLeft() {
        return dueDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    public boolean isOverdue() {
        return !done && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    public boolean isDueToday() {
        return !done && dueDate != null && dueDate.isEqual(LocalDate.now());
    }

    public String getPriorityText() {
        if ("HIGH".equals(priority)) {
            return "高";
        }
        if ("LOW".equals(priority)) {
            return "低";
        }
        return "中";
    }

    public String getPriorityClass() {
        if ("HIGH".equals(priority)) {
            return "danger";
        }
        if ("LOW".equals(priority)) {
            return "secondary";
        }
        return "warning";
    }

    public String getDeadlineText() {
        if (done) {
            return "已完成";
        }
        long days = getDaysLeft();
        if (days < 0) {
            return "已逾期 " + (-days) + " 天";
        }
        if (days == 0) {
            return "今天到期";
        }
        return "还有 " + days + " 天";
    }
}
