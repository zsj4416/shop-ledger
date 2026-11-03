<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<aside class="sidebar" id="sidebar">
    <a class="brand" href="${ctx}/dashboard">
        <span class="brand-mark"><i class="bi bi-journal-bookmark-fill"></i></span>
        <span>
            <span class="brand-name">小店账本</span>
            <span class="brand-sub">Shop Ledger</span>
        </span>
    </a>

    <div class="nav-label">经营</div>
    <nav class="sidebar-nav">
        <a class="nav-link ${activeNav == 'dashboard' ? 'active' : ''}" href="${ctx}/dashboard">
            <i class="bi bi-grid-1x2"></i><span>经营总览</span>
        </a>
        <a class="nav-link ${activeNav == 'transactions' ? 'active' : ''}" href="${ctx}/transactions">
            <i class="bi bi-receipt"></i><span>收支记账</span>
        </a>
        <a class="nav-link ${activeNav == 'reports' ? 'active' : ''}" href="${ctx}/reports">
            <i class="bi bi-bar-chart-line"></i><span>经营报表</span>
        </a>
    </nav>

    <div class="nav-label" style="margin-top:18px;">货品</div>
    <nav class="sidebar-nav">
        <a class="nav-link ${activeNav == 'products' ? 'active' : ''}" href="${ctx}/products">
            <i class="bi bi-box-seam"></i><span>商品档案</span>
        </a>
        <a class="nav-link ${activeNav == 'stock' ? 'active' : ''}" href="${ctx}/stock">
            <i class="bi bi-arrow-left-right"></i><span>库存流水</span>
        </a>
    </nav>

    <div class="nav-label" style="margin-top:18px;">管理</div>
    <nav class="sidebar-nav">
        <a class="nav-link ${activeNav == 'reminders' ? 'active' : ''}" href="${ctx}/reminders">
            <i class="bi bi-bell"></i><span>提醒中心</span>
        </a>
        <a class="nav-link ${activeNav == 'categories' ? 'active' : ''}" href="${ctx}/categories">
            <i class="bi bi-tags"></i><span>分类管理</span>
        </a>
    </nav>

    <div class="sidebar-foot">
        <div class="sidebar-tip">
            <strong>今日小提示</strong>
            <span id="sidebarTip">每天打烊前花两分钟核对收支，账目更安心。</span>
        </div>
        <div class="sidebar-user">
            <span class="avatar"><c:out value="${sessionScope.currentUser.displayName.substring(0,1)}"/></span>
            <span>
                <span class="name d-block"><c:out value="${sessionScope.currentUser.displayName}"/></span>
                <span class="role">${sessionScope.currentUser.admin ? '店铺管理员' : '店员'}</span>
            </span>
            <a class="logout" href="${ctx}/logout" title="退出登录"
               onclick="return confirm('确定要退出登录吗？');"><i class="bi bi-box-arrow-right"></i></a>
        </div>
    </div>
</aside>
