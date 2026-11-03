<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>

<div class="stat-grid mb-3">
    <div class="stat-card">
        <div class="stat-top">
            <span class="stat-label">今日收入</span>
            <span class="stat-icon green"><i class="bi bi-arrow-down-left"></i></span>
        </div>
        <div class="stat-value">¥ <fmt:formatNumber value="${stats.todayIncome}" pattern="#,##0.00"/></div>
        <div class="stat-foot">昨日 ¥<fmt:formatNumber value="${stats.yesterdayIncome}" pattern="#,##0.00"/></div>
    </div>
    <div class="stat-card">
        <div class="stat-top">
            <span class="stat-label">今日支出</span>
            <span class="stat-icon red"><i class="bi bi-arrow-up-right"></i></span>
        </div>
        <div class="stat-value">¥ <fmt:formatNumber value="${stats.todayExpense}" pattern="#,##0.00"/></div>
        <div class="stat-foot">今日结余
            <span class="${stats.todayProfit >= 0 ? 'up' : 'down'}">¥<fmt:formatNumber value="${stats.todayProfit}" pattern="#,##0.00"/></span>
        </div>
    </div>
    <div class="stat-card">
        <div class="stat-top">
            <span class="stat-label">${stats.monthName}结余</span>
            <span class="stat-icon amber"><i class="bi bi-wallet2"></i></span>
        </div>
        <div class="stat-value">¥ <fmt:formatNumber value="${stats.monthProfit}" pattern="#,##0.00"/></div>
        <div class="stat-foot">
            收 ¥<fmt:formatNumber value="${stats.monthIncome}" pattern="#,##0"/> ·
            支 ¥<fmt:formatNumber value="${stats.monthExpense}" pattern="#,##0"/>
        </div>
    </div>
    <div class="stat-card">
        <div class="stat-top">
            <span class="stat-label">库存成本价值</span>
            <span class="stat-icon ink"><i class="bi bi-box-seam"></i></span>
        </div>
        <div class="stat-value">¥ <fmt:formatNumber value="${stats.stockValue}" pattern="#,##0.00"/></div>
        <div class="stat-foot">
            ${stats.productCount} 种商品 ·
            <c:choose>
                <c:when test="${stats.lowStockCount > 0}">
                    <span class="down">${stats.lowStockCount} 项待补货</span>
                </c:when>
                <c:otherwise><span class="up">库存充足</span></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-xl-8">
        <div class="card h-100">
            <div class="card-header">
                <div>
                    <h2>近 14 天收支趋势</h2>
                    <div class="sub">收入与支出的每日变化</div>
                </div>
                <div class="ms-auto chart-legend">
                    <span><span class="dot" style="background:#2f9e6e"></span>收入</span>
                    <span><span class="dot" style="background:#d9534f"></span>支出</span>
                </div>
            </div>
            <div class="card-body">
                <div class="chart-wrap h-260"><canvas id="trendChart"></canvas></div>
            </div>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header">
                <div>
                    <h2>本月支出结构</h2>
                    <div class="sub">钱都花在了哪里</div>
                </div>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${empty stats.expensePieJson || stats.expensePieJson == '[]'}">
                        <div class="empty-state">
                            <i class="bi bi-pie-chart"></i>
                            <h3>本月还没有支出</h3>
                            <p>记一笔支出后，这里会出现分类占比</p>
                        </div>
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
    <div class="col-xl-8">
        <div class="card h-100">
            <div class="card-header">
                <h2>最近收支</h2>
                <a class="ms-auto tiny" href="${ctx}/transactions">查看全部 <i class="bi bi-arrow-right"></i></a>
            </div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                    <tr>
                        <th>日期</th>
                        <th>分类</th>
                        <th>备注</th>
                        <th>方式</th>
                        <th class="text-end">金额</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${stats.recentTransactions}" var="txn">
                        <tr>
                            <td class="nowrap tiny muted">${txn.monthDayText}</td>
                            <td>
                                <span class="badge-soft ${txn.income ? 'green' : 'red'}">
                                    <i class="bi ${txn.income ? 'bi-arrow-down-left' : 'bi-arrow-up-right'}"></i>${txn.categoryName}
                                </span>
                            </td>
                            <td class="text-truncate" style="max-width:220px;">${empty txn.remark ? '—' : txn.remark}</td>
                            <td class="tiny muted">${txn.paymentMethod}</td>
                            <td class="text-end amount ${txn.income ? 'income' : 'expense'}">
                                <span class="sign">${txn.income ? '+' : '-'}</span><fmt:formatNumber value="${txn.amount}" pattern="#,##0.00"/>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty stats.recentTransactions}">
                        <tr><td colspan="5">
                            <div class="empty-state">
                                <i class="bi bi-receipt-cutoff"></i>
                                <h3>还没有收支记录</h3>
                                <p>点击右上角「记一笔」开始记录</p>
                            </div>
                        </td></tr>
                    </c:if>
                    </tbody>
                </table>
        <div class="table-hint d-md-none"><i class="bi bi-arrows-expand me-1"></i>左右滑动查看完整表格</div>
            </div>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card h-100">
            <div class="card-header">
                <h2>库存预警</h2>
                <span class="badge-soft amber ms-auto">${stats.lowStockCount} 项</span>
            </div>
            <div class="pt-1 pb-1">
                <c:forEach items="${stats.lowStockProducts}" var="p">
                    <div class="stock-alert-row">
                        <div class="flex-grow-1 min-w-0">
                            <div class="cell-title text-truncate">${p.name}</div>
                            <div class="cell-sub">${p.category} · 阈值 ${p.alertThreshold}${p.unit}</div>
                        </div>
                        <div class="text-end">
                            <div class="amount ${p.outOfStock ? 'expense' : ''}">${p.stock}<small class="fw-normal">${p.unit}</small></div>
                            <div class="stock-bar"><span class="${p.outOfStock ? 'critical' : ''}"
                                style="width:${p.alertThreshold > 0 ? (p.stock * 100 / p.alertThreshold > 100 ? 100 : p.stock * 100 / p.alertThreshold) : 0}%"></span></div>
                        </div>
                        <a class="btn btn-sm btn-outline-secondary" href="${ctx}/stock?productId=${p.id}&open=in">补货</a>
                    </div>
                </c:forEach>
                <c:if test="${empty stats.lowStockProducts}">
                    <div class="empty-state">
                        <i class="bi bi-check2-circle"></i>
                        <h3>库存都很充足</h3>
                        <p>低于预警阈值的商品会出现在这里</p>
                    </div>
                </c:if>
            </div>
            <div class="card-footer tiny muted d-flex justify-content-between">
                <span>本月入库 ${stats.inboundThisMonth} 件</span>
                <span>出库 ${stats.outboundThisMonth} 件</span>
            </div>
        </div>
    </div>
</div>

<div class="row g-3">
    <div class="col-xl-7">
        <div class="card h-100">
            <div class="card-header">
                <h2>待办与提醒</h2>
                <a class="ms-auto tiny" href="${ctx}/reminders">管理提醒 <i class="bi bi-arrow-right"></i></a>
            </div>
            <div class="pt-1 pb-1">
                <c:forEach items="${stats.upcomingReminders}" var="r">
                    <form class="reminder-item" method="post" action="${ctx}/reminders">
                        <input type="hidden" name="action" value="toggle">
                        <input type="hidden" name="id" value="${r.id}">
                        <input type="hidden" name="done" value="1">
                        <input type="hidden" name="back" value="/dashboard">
                        <button class="reminder-check" type="submit" title="标记为完成" aria-label="标记为完成">
                            <i class="bi bi-check-lg"></i>
                        </button>
                        <div class="flex-grow-1">
                            <div class="r-title">${r.title}</div>
                            <c:if test="${not empty r.content}"><div class="r-content">${r.content}</div></c:if>
                            <div class="r-meta">
                                <span class="badge-soft ${r.priorityClass}">${r.priorityText}</span>
                                <span class="r-due"><i class="bi bi-calendar3 me-1"></i>${r.dueDateText}</span>
                                <span class="r-due ${r.overdue ? 'text-expense' : ''}">${r.deadlineText}</span>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-ghost reminder-done" type="submit">完成</button>
                    </form>
                </c:forEach>
                <c:if test="${empty stats.upcomingReminders}">
                    <div class="empty-state">
                        <i class="bi bi-bell-slash"></i>
                        <h3>近 7 天没有待办</h3>
                        <p>可以到提醒中心添加交租、对账等事项</p>
                    </div>
                </c:if>
            </div>
        </div>
    </div>
    <div class="col-xl-5">
        <div class="card h-100">
            <div class="card-header">
                <h2>本月热销</h2>
                <span class="sub ms-auto">按销售数量</span>
            </div>
            <div class="table-responsive">
                <table class="table">
                    <thead><tr><th>商品</th><th class="text-end">数量</th><th class="text-end">销售额</th></tr></thead>
                    <tbody>
                    <c:forEach items="${stats.topProducts}" var="row" varStatus="st">
                        <tr>
                            <td><span class="badge-soft gray me-2">${st.index + 1}</span>${row[0]}</td>
                            <td class="text-end">${row[1]}</td>
                            <td class="text-end amount">¥<fmt:formatNumber value="${row[2]}" pattern="#,##0.00"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty stats.topProducts}">
                        <tr><td colspan="3">
                            <div class="empty-state">
                                <i class="bi bi-graph-up"></i>
                                <h3>本月还没有销售出库</h3>
                                <p>在库存流水中记录出库后会统计到这里</p>
                            </div>
                        </td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        if (!window.Chart) { return; }
        Chart.defaults.font.family = '"PingFang SC","Microsoft YaHei",system-ui,sans-serif';
        Chart.defaults.color = '#7c857f';

        var trendCtx = document.getElementById('trendChart');
        if (trendCtx) {
            var labels = ${stats.trendLabelsJson};
            var income = ${stats.trendIncomeJson};
            var expense = ${stats.trendExpenseJson};
            new Chart(trendCtx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: '收入', data: income, borderColor: '#2f9e6e',
                            backgroundColor: 'rgba(47,158,110,.10)', fill: true, tension: .35,
                            borderWidth: 2.2, pointRadius: 0, pointHoverRadius: 4
                        },
                        {
                            label: '支出', data: expense, borderColor: '#d9534f',
                            backgroundColor: 'rgba(217,83,79,.07)', fill: true, tension: .35,
                            borderWidth: 2.2, pointRadius: 0, pointHoverRadius: 4
                        }
                    ]
                },
                options: {
                    responsive: true, maintainAspectRatio: false,
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            backgroundColor: '#16241f', padding: 10, cornerRadius: 8,
                            callbacks: {
                                label: function (ctx) {
                                    return ctx.dataset.label + '：¥ ' + Number(ctx.parsed.y).toFixed(2);
                                }
                            }
                        }
                    },
                    scales: {
                        x: { grid: { display: false }, border: { display: false }, ticks: { maxTicksLimit: 7, font: { size: 10 } } },
                        y: {
                            beginAtZero: true, border: { display: false },
                            grid: { color: 'rgba(22,36,31,.06)' },
                            ticks: { font: { size: 10 }, callback: function (v) { return '¥' + v; } }
                        }
                    }
                }
            });
        }

        var pieCtx = document.getElementById('expensePie');
        if (pieCtx) {
            var pie = ${stats.expensePieJson};
            new Chart(pieCtx, {
                type: 'doughnut',
                data: {
                    labels: pie.map(function (i) { return i.name; }),
                    datasets: [{
                        data: pie.map(function (i) { return i.value; }),
                        backgroundColor: ['#2f9e6e', '#e8a33d', '#5b8def', '#d9534f', '#8e7cc3', '#4fb3bf', '#c9a227', '#9aa39d'],
                        borderWidth: 0, hoverOffset: 4
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false, cutout: '64%',
                    plugins: {
                        legend: { position: 'bottom', labels: { boxWidth: 8, boxHeight: 8, usePointStyle: true, font: { size: 11 }, padding: 12 } },
                        tooltip: {
                            backgroundColor: '#16241f', padding: 10, cornerRadius: 8,
                            callbacks: {
                                label: function (ctx) { return ctx.label + '：¥ ' + Number(ctx.parsed).toFixed(2); }
                            }
                        }
                    }
                }
            });
        }
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
