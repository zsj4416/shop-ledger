<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>
<style>
    .cat-row { padding: 9px 0; border-bottom: 1px dashed var(--line-soft); }
    .cat-row:last-child { border-bottom: 0; }
    .cat-row .cat-head { display: flex; justify-content: space-between; font-size: 12.5px; margin-bottom: 5px; }
    .cat-row .cat-name { font-weight: 600; }
    .cat-row .cat-amt { font-variant-numeric: tabular-nums; font-weight: 600; }
    .cat-bar { height: 5px; border-radius: 99px; background: #f0ece2; overflow: hidden; }
    .cat-bar > span { display: block; height: 100%; border-radius: 99px; background: var(--accent); }
    .cat-row.expense .cat-bar > span { background: var(--danger); }
    .cat-pct { font-size: 10.5px; color: var(--muted); margin-left: 6px; }
</style>

<div class="filter-bar">
    <form method="get" action="${ctx}/reports" class="row g-2 align-items-end">
        <div class="col-6 col-md-3">
            <label class="form-label">开始日期</label>
            <input class="form-control" type="date" name="start" value="${filterStart}">
        </div>
        <div class="col-6 col-md-3">
            <label class="form-label">结束日期</label>
            <input class="form-control" type="date" name="end" value="${filterEnd}">
        </div>
        <div class="col-12 col-md-6 d-flex gap-2 flex-wrap">
            <button class="btn btn-dark btn-sm" type="submit"><i class="bi bi-search me-1"></i>生成报表</button>
            <div class="quick-ranges">
                <button class="btn btn-outline-secondary" type="button" data-range="month">本月</button>
                <button class="btn btn-outline-secondary" type="button" data-range="last-month">上月</button>
                <button class="btn btn-outline-secondary" type="button" data-range="30">近 30 天</button>
                <button class="btn btn-outline-secondary" type="button" data-range="90">近 90 天</button>
                <button class="btn btn-outline-secondary" type="button" data-range="year">今年</button>
            </div>
            <c:url var="csvUrl" value="/reports">
                <c:param name="export" value="csv"/>
                <c:param name="start" value="${filterStart}"/>
                <c:param name="end" value="${filterEnd}"/>
            </c:url>
            <a class="btn btn-outline-secondary btn-sm ms-md-auto" href="${csvUrl}">
                <i class="bi bi-download me-1"></i>导出 CSV
            </a>
            <button class="btn btn-outline-secondary btn-sm" type="button" onclick="window.print()">
                <i class="bi bi-printer me-1"></i>打印
            </button>
        </div>
    </form>
</div>

<div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
    <span class="badge-soft gray"><i class="bi bi-calendar3"></i>${report.start} 至 ${report.end}</span>
    <span class="tiny muted">共 ${report.transactionCount} 笔收支记录</span>
</div>

<div class="row g-3 mb-3">
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top"><span class="stat-label">总收入</span>
                <span class="stat-icon green"><i class="bi bi-arrow-down-left"></i></span></div>
            <div class="stat-value text-income">¥ <fmt:formatNumber value="${report.income}" pattern="#,##0.00"/></div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top"><span class="stat-label">总支出</span>
                <span class="stat-icon red"><i class="bi bi-arrow-up-right"></i></span></div>
            <div class="stat-value text-expense">¥ <fmt:formatNumber value="${report.expense}" pattern="#,##0.00"/></div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top"><span class="stat-label">经营结余</span>
                <span class="stat-icon amber"><i class="bi bi-wallet2"></i></span></div>
            <div class="stat-value">¥ <fmt:formatNumber value="${report.profit}" pattern="#,##0.00"/></div>
            <div class="stat-foot">${report.profit >= 0 ? '保持盈利，继续加油' : '本期支出高于收入'}</div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top"><span class="stat-label">笔均金额</span>
                <span class="stat-icon ink"><i class="bi bi-calculator"></i></span></div>
            <div class="stat-value">¥ <fmt:formatNumber
                    value="${report.avgAmount}"
                    pattern="#,##0.00"/></div>
            <div class="stat-foot">收入 + 支出 ÷ 笔数</div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-xl-8">
        <div class="card h-100">
            <div class="card-header">
                <div><h2>每日收支趋势</h2><div class="sub">所选日期范围内每天的收入与支出</div></div>
                <div class="ms-auto chart-legend">
                    <span><span class="dot" style="background:#2f9e6e"></span>收入</span>
                    <span><span class="dot" style="background:#d9534f"></span>支出</span>
                </div>
            </div>
            <div class="card-body">
                <div class="chart-wrap h-260"><canvas id="dailyChart"></canvas></div>
            </div>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header"><div><h2>支出分类占比</h2><div class="sub">按支出金额排序</div></div></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty report.expenseCategories}">
                        <div class="empty-state"><i class="bi bi-pie-chart"></i><h3>本期没有支出</h3><p>选择含支出的日期范围试试</p></div>
                    </c:when>
                    <c:otherwise>
                        <div class="chart-wrap h-220"><canvas id="expensePie"></canvas></div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-xl-7">
        <div class="card h-100">
            <div class="card-header">
                <div><h2>近 6 个月收支</h2><div class="sub">以结束日期所在月份为基准</div></div>
                <div class="ms-auto chart-legend">
                    <span><span class="dot" style="background:#2f9e6e"></span>收入</span>
                    <span><span class="dot" style="background:#e8a33d"></span>支出</span>
                </div>
            </div>
            <div class="card-body">
                <div class="chart-wrap h-230"><canvas id="monthlyChart"></canvas></div>
            </div>
        </div>
    </div>
    <div class="col-xl-5">
        <div class="card">
            <div class="card-header"><h2>热销商品</h2><span class="sub ms-auto">按销售额</span></div>
            <div class="table-responsive">
                <table class="table">
                    <thead><tr><th>商品</th><th class="text-end">销量</th><th class="text-end">销售额</th></tr></thead>
                    <tbody>
                    <c:forEach items="${report.topProducts}" var="row" varStatus="st">
                        <tr>
                            <td><span class="badge-soft gray me-2">${st.index + 1}</span>${row[0]}</td>
                            <td class="text-end">${row[1]}</td>
                            <td class="text-end amount">¥<fmt:formatNumber value="${row[2]}" pattern="#,##0.00"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty report.topProducts}">
                        <tr><td colspan="3"><div class="empty-state"><i class="bi bi-graph-up"></i><h3>本期没有销售出库</h3><p>库存出库后会自动统计</p></div></td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<div class="row g-3">
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header"><h2>收入构成</h2><span class="sub ms-auto">合计 ¥<fmt:formatNumber value="${report.income}" pattern="#,##0.00"/></span></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty report.incomeCategories}">
                        <div class="empty-state py-4"><i class="bi bi-inbox"></i><h3>本期没有收入</h3></div>
                    </c:when>
                    <c:otherwise>
                        <div class="cat-list" data-total="${report.income}">
                            <c:forEach items="${report.incomeCategories}" var="row">
                                <div class="cat-row" data-amount="${row[1]}">
                                    <div class="cat-head">
                                        <span class="cat-name">${row[0]}</span>
                                        <span><span class="cat-amt">¥<fmt:formatNumber value="${row[1]}" pattern="#,##0.00"/></span><span class="cat-pct"></span></span>
                                    </div>
                                    <div class="cat-bar"><span style="width:0"></span></div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header"><h2>支出构成</h2><span class="sub ms-auto">合计 ¥<fmt:formatNumber value="${report.expense}" pattern="#,##0.00"/></span></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty report.expenseCategories}">
                        <div class="empty-state py-4"><i class="bi bi-inbox"></i><h3>本期没有支出</h3></div>
                    </c:when>
                    <c:otherwise>
                        <div class="cat-list" data-total="${report.expense}">
                            <c:forEach items="${report.expenseCategories}" var="row">
                                <div class="cat-row expense" data-amount="${row[1]}">
                                    <div class="cat-head">
                                        <span class="cat-name">${row[0]}</span>
                                        <span><span class="cat-amt">¥<fmt:formatNumber value="${row[1]}" pattern="#,##0.00"/></span><span class="cat-pct"></span></span>
                                    </div>
                                    <div class="cat-bar"><span style="width:0"></span></div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        // 分类占比条
        document.querySelectorAll('.cat-list').forEach(function (list) {
            var total = Number(list.getAttribute('data-total')) || 0;
            list.querySelectorAll('.cat-row').forEach(function (row) {
                var amount = Number(row.getAttribute('data-amount')) || 0;
                var pct = total > 0 ? (amount / total * 100) : 0;
                row.querySelector('.cat-bar > span').style.width = Math.max(pct, amount > 0 ? 2 : 0) + '%';
                row.querySelector('.cat-pct').textContent = total > 0 ? pct.toFixed(1) + '%' : '';
            });
        });

        if (!window.Chart) { return; }
        Chart.defaults.font.family = '"PingFang SC","Microsoft YaHei",system-ui,sans-serif';
        Chart.defaults.color = '#7c857f';
        var money = function (v) { return '¥ ' + Number(v).toFixed(2); };

        var dailyCtx = document.getElementById('dailyChart');
        if (dailyCtx) {
            new Chart(dailyCtx, {
                type: 'line',
                data: {
                    labels: ${report.dailyLabelsJson},
                    datasets: [
                        { label: '收入', data: ${report.incomeSeriesJson}, borderColor: '#2f9e6e',
                          backgroundColor: 'rgba(47,158,110,.10)', fill: true, tension: .3, borderWidth: 2, pointRadius: 0, pointHoverRadius: 4 },
                        { label: '支出', data: ${report.expenseSeriesJson}, borderColor: '#d9534f',
                          backgroundColor: 'rgba(217,83,79,.07)', fill: true, tension: .3, borderWidth: 2, pointRadius: 0, pointHoverRadius: 4 }
                    ]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, interaction: { mode: 'index', intersect: false },
                    plugins: { legend: { display: false }, tooltip: { backgroundColor: '#16241f', padding: 10, cornerRadius: 8,
                        callbacks: { label: function (c) { return c.dataset.label + '：' + money(c.parsed.y); } } } },
                    scales: {
                        x: { grid: { display: false }, border: { display: false }, ticks: { maxTicksLimit: 8, font: { size: 10 } } },
                        y: { beginAtZero: true, border: { display: false }, grid: { color: 'rgba(22,36,31,.06)' },
                             ticks: { font: { size: 10 }, callback: function (v) { return '¥' + v; } } }
                    }
                }
            });
        }

        var pieCtx = document.getElementById('expensePie');
        if (pieCtx) {
            var pie = ${report.expenseCategoryJson};
            new Chart(pieCtx, {
                type: 'doughnut',
                data: { labels: pie.map(function (i) { return i.name; }),
                        datasets: [{ data: pie.map(function (i) { return i.value; }),
                                     backgroundColor: ['#d9534f', '#e8a33d', '#2f9e6e', '#5b8def', '#8e7cc3', '#4fb3bf', '#c9a227', '#9aa39d'],
                                     borderWidth: 0, hoverOffset: 4 }] },
                options: { responsive: true, maintainAspectRatio: false, cutout: '62%',
                    plugins: { legend: { position: 'bottom', labels: { boxWidth: 8, boxHeight: 8, usePointStyle: true, font: { size: 10.5 }, padding: 10 } },
                               tooltip: { backgroundColor: '#16241f', padding: 10, cornerRadius: 8,
                                   callbacks: { label: function (c) { return c.label + '：' + money(c.parsed); } } } } }
            });
        }

        var monthlyCtx = document.getElementById('monthlyChart');
        if (monthlyCtx) {
            new Chart(monthlyCtx, {
                type: 'bar',
                data: {
                    labels: ${report.monthLabelsJson}.map(function (m) { return m.replace('-', ' 年 ') + ' 月'; }),
                    datasets: [
                        { label: '收入', data: ${report.monthIncomeJson}, backgroundColor: '#2f9e6e', borderRadius: 5, maxBarThickness: 22 },
                        { label: '支出', data: ${report.monthExpenseJson}, backgroundColor: '#e8a33d', borderRadius: 5, maxBarThickness: 22 }
                    ]
                },
                options: { responsive: true, maintainAspectRatio: false,
                    plugins: { legend: { display: false }, tooltip: { backgroundColor: '#16241f', padding: 10, cornerRadius: 8,
                        callbacks: { label: function (c) { return c.dataset.label + '：' + money(c.parsed.y); } } } },
                    scales: { x: { grid: { display: false }, border: { display: false }, ticks: { font: { size: 10 } } },
                              y: { beginAtZero: true, border: { display: false }, grid: { color: 'rgba(22,36,31,.06)' },
                                   ticks: { font: { size: 10 }, callback: function (v) { return '¥' + v; } } } } }
            });
        }

        // 快捷时间范围
        document.querySelectorAll('[data-range]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var start = document.querySelector('input[name="start"]');
                var end = document.querySelector('input[name="end"]');
                var now = new Date();
                var fmt = function (d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); };
                var range = btn.getAttribute('data-range');
                if (range === 'month') {
                    start.value = fmt(new Date(now.getFullYear(), now.getMonth(), 1));
                    end.value = fmt(new Date(now.getFullYear(), now.getMonth() + 1, 0));
                } else if (range === 'last-month') {
                    start.value = fmt(new Date(now.getFullYear(), now.getMonth() - 1, 1));
                    end.value = fmt(new Date(now.getFullYear(), now.getMonth(), 0));
                } else if (range === '30' || range === '90') {
                    var days = range === '30' ? 29 : 89;
                    start.value = fmt(new Date(now.getTime() - days * 86400000));
                    end.value = fmt(now);
                } else {
                    start.value = fmt(new Date(now.getFullYear(), 0, 1));
                    end.value = fmt(now);
                }
                start.form.submit();
            });
        });
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
