<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#10241c">
    <title>登录 · 小店账本</title>
    <link rel="icon" type="image/svg+xml" href="${ctx}/assets/favicon.svg">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
    <style>
        .mock-book {
            position: relative; z-index: 1; width: 320px; margin-top: 44px;
            background: #fdfbf4; border-radius: 16px; padding: 18px 20px 20px;
            box-shadow: 0 24px 60px rgba(0, 0, 0, .35); transform: rotate(-2deg);
            color: var(--ink);
        }
        .mock-book .mb-head { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #ddd6c4; padding-bottom: 10px; }
        .mock-book .mb-title { font-size: 12px; font-weight: 700; letter-spacing: 1px; color: #6c756f; }
        .mock-book .mb-date { font-size: 10px; color: #9aa39d; }
        .mock-row { display: flex; align-items: center; justify-content: space-between; padding: 7px 0; font-size: 11.5px; border-bottom: 1px solid #f2eee2; }
        .mock-row:last-child { border-bottom: 0; }
        .mock-row .m-name { color: #4a554f; }
        .mock-row .m-tag { font-size: 9px; padding: 1px 6px; border-radius: 99px; background: #e6f4ee; color: #24805a; margin-right: 6px; }
        .mock-row .m-tag.exp { background: #fdecea; color: #c24a46; }
        .mock-row .m-amt { font-weight: 700; color: #24805a; }
        .mock-row .m-amt.exp { color: #c24a46; }
        .mock-total { display: flex; justify-content: space-between; margin-top: 12px; padding-top: 10px; border-top: 1px solid #ddd6c4; font-size: 11px; color: #6c756f; }
        .mock-total strong { color: #16241f; font-size: 15px; }
        @media (min-width: 992px) {
            .login-visual { justify-content: flex-start; }
            .login-headline { margin-top: 54px; }
        }
    </style>
</head>
<body>
<div class="login-shell">
    <section class="login-visual">
        <div class="brand">
            <span class="brand-mark"><i class="bi bi-journal-bookmark-fill"></i></span>
            <span>
                <span class="brand-name">小店账本</span>
                <span class="brand-sub">Shop Ledger</span>
            </span>
        </div>
        <h1 class="login-headline">小店的每一笔，<br>都<em>算得清楚</em>。</h1>
        <p class="login-desc">
            为街角小店准备的轻量经营账本。收支记账、库存进出、经营报表与到期提醒，
            一个页面全部照顾到，不装插件、不记复杂公式。
        </p>

        <div class="login-features">
            <div class="login-feature">
                <i class="bi bi-receipt"></i>
                <div><div class="t">收支记账</div><div class="s">收入支出分类清晰</div></div>
            </div>
            <div class="login-feature">
                <i class="bi bi-box-seam"></i>
                <div><div class="t">库存管理</div><div class="s">进销存与预警</div></div>
            </div>
            <div class="login-feature">
                <i class="bi bi-bar-chart-line"></i>
                <div><div class="t">经营报表</div><div class="s">趋势与分类占比</div></div>
            </div>
            <div class="login-feature">
                <i class="bi bi-bell"></i>
                <div><div class="t">到期提醒</div><div class="s">待办不再遗漏</div></div>
            </div>
        </div>

        <div class="mock-book">
            <div class="mb-head">
                <span class="mb-title">今日流水</span>
                <span class="mb-date"><i class="bi bi-calendar3 me-1"></i>营业中</span>
            </div>
            <div class="mock-row">
                <span class="m-name"><span class="m-tag">收入</span>门店零售</span>
                <span class="m-amt">+ 386.00</span>
            </div>
            <div class="mock-row">
                <span class="m-name"><span class="m-tag exp">支出</span>进货 · 饮料</span>
                <span class="m-amt exp">- 128.50</span>
            </div>
            <div class="mock-row">
                <span class="m-name"><span class="m-tag">收入</span>会员充值</span>
                <span class="m-amt">+ 200.00</span>
            </div>
            <div class="mock-total"><span>今日结余</span><strong>¥ 457.50</strong></div>
        </div>

        <div class="login-foot">Jakarta EE · Servlet + JSP + MySQL · 本地部署，数据留在自己手里</div>
    </section>

    <section class="login-panel">
        <div class="login-card">
            <div class="welcome">欢迎回来</div>
            <div class="sub">登录后继续打理你的小店</div>

            <c:if test="${not empty loginError}">
                <div class="alert alert-danger d-flex align-items-center gap-2">
                    <i class="bi bi-exclamation-circle"></i><span>${loginError}</span>
                </div>
            </c:if>

            <form method="post" action="${ctx}/login" autocomplete="on">
                <input type="hidden" name="redirect" value="${param.redirect}">
                <div class="mb-3">
                    <label class="form-label" for="username">账号</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-person"></i></span>
                        <input class="form-control" id="username" name="username" type="text"
                               value="${empty username ? 'admin' : username}" placeholder="请输入账号"
                               autocomplete="username" required autofocus>
                    </div>
                </div>
                <div class="mb-3">
                    <label class="form-label" for="password">密码</label>
                    <div class="input-group">
                        <span class="input-group-text"><i class="bi bi-lock"></i></span>
                        <input class="form-control" id="password" name="password" type="password"
                               placeholder="请输入密码" autocomplete="current-password" required>
                        <button class="btn btn-outline-secondary" type="button" id="togglePassword" tabindex="-1" aria-label="显示密码">
                            <i class="bi bi-eye" id="eyeIcon"></i>
                        </button>
                    </div>
                </div>
                <button class="btn btn-primary w-100 mt-2" type="submit">
                    <i class="bi bi-box-arrow-in-right me-1"></i> 登录小店账本
                </button>
            </form>

            <div class="demo-hint">
                <i class="bi bi-info-circle me-1"></i>演示账号：<code>admin</code>　密码：<code>admin123</code><br>
                首次启动会自动创建数据库与演示数据，可直接登录体验。
            </div>
        </div>
    </section>
</div>

<script>
    (function () {
        var pwd = document.getElementById('password');
        var btn = document.getElementById('togglePassword');
        var icon = document.getElementById('eyeIcon');
        if (btn) {
            btn.addEventListener('click', function () {
                var show = pwd.type === 'password';
                pwd.type = show ? 'text' : 'password';
                icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
            });
        }
    })();
</script>
</body>
</html>
