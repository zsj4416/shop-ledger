<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#10241c">
    <title>${pageTitle} · 小店账本</title>
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css?v=20261021d">
</head>
<body>
<div class="app-shell">
    <jsp:include page="/WEB-INF/views/common/sidebar.jsp"/>
    <div class="sidebar-overlay" id="sidebarOverlay"></div>
    <div class="app-main">
        <jsp:include page="/WEB-INF/views/common/topbar.jsp"/>
        <main class="content page-fade">
            <c:if test="${not empty sessionScope.flashMessage}">
                <div class="alert alert-${sessionScope.flashType} alert-dismissible fade show alert-auto" role="alert">
                    <i class="bi bi-info-circle me-1"></i><c:out value="${sessionScope.flashMessage}"/>
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="关闭"></button>
                </div>
                <c:remove var="flashMessage" scope="session"/>
                <c:remove var="flashType" scope="session"/>
            </c:if>
            <c:if test="${applicationScope.appReady == false}">
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle me-1"></i>
                    数据库初始化未完成：<c:out value="${applicationScope.appError}"/>。请检查 MySQL 服务与 db.properties 配置后重启应用。
                </div>
            </c:if>
