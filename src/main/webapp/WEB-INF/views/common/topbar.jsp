<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<header class="topbar">
    <button class="icon-btn mobile-toggle" type="button" data-sidebar-toggle aria-label="打开菜单">
        <i class="bi bi-list"></i>
    </button>
    <div class="topbar-title">
        <h1>${pageTitle}</h1>
        <p>
            <c:choose>
                <c:when test="${activeNav == 'dashboard'}">今天也把小店经营得井井有条</c:when>
                <c:when test="${activeNav == 'transactions'}">每一笔收入与支出，都清清楚楚</c:when>
                <c:when test="${activeNav == 'reports'}">用数据看见小店的经营节奏</c:when>
                <c:when test="${activeNav == 'products'}">商品、进价与售价，一处管理</c:when>
                <c:when test="${activeNav == 'stock'}">入库、出库与盘点，账实相符</c:when>
                <c:when test="${activeNav == 'reminders'}">待办事项与库存预警，不再遗漏</c:when>
                <c:when test="${activeNav == 'categories'}">维护收支分类，报表更清晰</c:when>
                <c:otherwise>小店经营，心中有数</c:otherwise>
            </c:choose>
        </p>
    </div>
    <div class="topbar-spacer"></div>
    <div class="topbar-date">
        <div class="d" id="topbarDate"></div>
        <div class="w" id="topbarWeekday"></div>
    </div>
    <a class="icon-btn" href="${ctx}/reminders" title="提醒中心">
        <i class="bi bi-bell"></i>
    </a>
    <a class="btn-quick" href="${ctx}/transactions?open=1">
        <i class="bi bi-plus-lg"></i><span>记一笔</span>
    </a>
</header>
