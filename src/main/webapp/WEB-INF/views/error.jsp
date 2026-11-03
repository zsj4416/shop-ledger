<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="status" value="${requestScope['jakarta.servlet.error.status_code']}"/>
<c:set var="errorUri" value="${requestScope['jakarta.servlet.error.request_uri']}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${empty status ? '出错了' : status} · 小店账本</title>
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body style="background:var(--bg);">
<div class="d-flex align-items-center justify-content-center" style="min-height:100vh;padding:24px;">
    <div class="card" style="max-width:520px;width:100%;">
        <div class="card-body p-4 p-md-5 text-center">
            <div class="d-inline-flex align-items-center justify-content-center mb-3"
                 style="width:74px;height:74px;border-radius:22px;background:var(--amber-soft);color:#a86c10;">
                <i class="bi bi-exclamation-triangle" style="font-size:34px;"></i>
            </div>
            <div class="fw-bold" style="font-size:46px;line-height:1;letter-spacing:-2px;">${empty status ? '500' : status}</div>
            <h1 class="fw-bold mt-3 mb-2" style="font-size:18px;">
                <c:choose>
                    <c:when test="${status == 404}">这个页面找不到了</c:when>
                    <c:when test="${status == 403}">没有访问权限</c:when>
                    <c:otherwise>页面开小差了</c:otherwise>
                </c:choose>
            </h1>
            <p class="muted mb-1" style="font-size:13px;">
                <c:choose>
                    <c:when test="${status == 404}">链接可能已经失效，或者地址输入有误。</c:when>
                    <c:otherwise>服务器处理请求时出现异常，请稍后重试；若持续出现，请检查数据库连接与应用日志。</c:otherwise>
                </c:choose>
            </p>
            <c:if test="${not empty errorUri}">
                <p class="tiny muted text-break mb-4">${errorUri}</p>
            </c:if>
            <div class="d-flex gap-2 justify-content-center mt-4">
                <button class="btn btn-outline-secondary" type="button" onclick="history.back()">
                    <i class="bi bi-arrow-left me-1"></i>返回上一页
                </button>
                <c:choose>
                    <c:when test="${not empty sessionScope.currentUser}">
                        <a class="btn btn-primary" href="${ctx}/dashboard"><i class="bi bi-house me-1"></i>回到总览</a>
                    </c:when>
                    <c:otherwise>
                        <a class="btn btn-primary" href="${ctx}/login"><i class="bi bi-box-arrow-in-right me-1"></i>去登录</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>
</body>
</html>
