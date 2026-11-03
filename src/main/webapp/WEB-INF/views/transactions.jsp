<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="qs" value="${pageContext.request.queryString}"/>
<c:set var="backPath" value="/transactions${empty qs ? '' : '?'}${qs}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>

<div class="filter-bar">
    <form method="get" action="${ctx}/transactions" class="row g-2 align-items-end">
        <div class="col-6 col-md-2">
            <label class="form-label">收支类型</label>
            <select class="form-select" name="type">
                <option value="">全部类型</option>
                <option value="INCOME" ${filterType == 'INCOME' ? 'selected' : ''}>收入</option>
                <option value="EXPENSE" ${filterType == 'EXPENSE' ? 'selected' : ''}>支出</option>
            </select>
        </div>
        <div class="col-6 col-md-3">
            <label class="form-label">分类</label>
            <select class="form-select" name="categoryId">
                <option value="">全部分类</option>
                <c:forEach items="${categories}" var="cat">
                    <option value="${cat.id}" ${cat.id == filterCategoryId ? 'selected' : ''}>
                        ${cat.name}（${cat.income ? '收入' : '支出'}）
                    </option>
                </c:forEach>
            </select>
        </div>
        <div class="col-6 col-md-2">
            <label class="form-label">开始日期</label>
            <input class="form-control" type="date" name="start" value="${filterStart}">
        </div>
        <div class="col-6 col-md-2">
            <label class="form-label">结束日期</label>
            <input class="form-control" type="date" name="end" value="${filterEnd}">
        </div>
        <div class="col-12 col-md-3">
            <label class="form-label">备注关键词</label>
            <input class="form-control" type="search" name="keyword" value="${filterKeyword}" placeholder="搜索备注内容">
        </div>
        <div class="col-12 d-flex flex-wrap align-items-center gap-2 mt-2">
            <button class="btn btn-dark btn-sm" type="submit"><i class="bi bi-search me-1"></i>查询</button>
            <a class="btn btn-outline-secondary btn-sm" href="${ctx}/transactions">重置</a>
            <div class="quick-ranges ms-md-2">
                <button class="btn btn-outline-secondary" type="button" data-range="month">本月</button>
                <button class="btn btn-outline-secondary" type="button" data-range="30">近 30 天</button>
                <button class="btn btn-outline-secondary" type="button" data-range="today">今天</button>
                <button class="btn btn-outline-secondary" type="button" data-range="last-month">上月</button>
            </div>
            <button class="btn btn-primary btn-sm ms-auto" type="button" onclick="openTxn()">
                <i class="bi bi-plus-lg me-1"></i>新增记录
            </button>
        </div>
    </form>
</div>

<div class="row g-2 mb-3">
    <div class="col-md-4">
        <div class="card"><div class="card-body py-3 d-flex align-items-center gap-3">
            <span class="stat-icon green"><i class="bi bi-arrow-down-left"></i></span>
            <div>
                <div class="tiny muted">筛选范围收入</div>
                <div class="amount income fs-5">¥ <fmt:formatNumber value="${totalIncome}" pattern="#,##0.00"/></div>
            </div>
        </div></div>
    </div>
    <div class="col-md-4">
        <div class="card"><div class="card-body py-3 d-flex align-items-center gap-3">
            <span class="stat-icon red"><i class="bi bi-arrow-up-right"></i></span>
            <div>
                <div class="tiny muted">筛选范围支出</div>
                <div class="amount expense fs-5">¥ <fmt:formatNumber value="${totalExpense}" pattern="#,##0.00"/></div>
            </div>
        </div></div>
    </div>
    <div class="col-md-4">
        <div class="card"><div class="card-body py-3 d-flex align-items-center gap-3">
            <span class="stat-icon amber"><i class="bi bi-calculator"></i></span>
            <div>
                <div class="tiny muted">筛选范围结余</div>
                <div class="amount fs-5">¥ <fmt:formatNumber value="${totalIncome - totalExpense}" pattern="#,##0.00"/></div>
            </div>
        </div></div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h2>收支明细</h2>
        <span class="sub">共 ${pageData.total} 笔记录</span>
    </div>
    <div class="table-responsive">
        <table class="table align-middle">
            <thead>
            <tr>
                <th style="width:110px;">日期</th>
                <th>分类</th>
                <th>备注</th>
                <th>支付方式</th>
                <th>记录人</th>
                <th class="text-end">金额</th>
                <th class="text-end" style="width:110px;">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${pageData.items}" var="txn">
                <tr>
                    <td class="nowrap">${txn.txnDate}</td>
                    <td>
                        <span class="badge-soft ${txn.income ? 'green' : 'red'}">
                            <i class="bi ${txn.income ? 'bi-arrow-down-left' : 'bi-arrow-up-right'}"></i>${txn.categoryName}
                        </span>
                    </td>
                    <td style="max-width:300px;">
                        <div class="text-truncate">${empty txn.remark ? '—' : txn.remark}</div>
                    </td>
                    <td class="tiny muted">${txn.paymentMethod}</td>
                    <td class="tiny muted">${txn.createdByName}</td>
                    <td class="text-end amount ${txn.income ? 'income' : 'expense'}">
                        <span class="sign">${txn.income ? '+' : '-'}</span><fmt:formatNumber value="${txn.amount}" pattern="#,##0.00"/>
                    </td>
                    <td class="text-end hover-actions nowrap">
                        <button class="btn btn-sm btn-ghost" type="button" title="编辑"
                                data-txn-id="${txn.id}" data-txn-type="${txn.type}"
                                data-txn-category="${txn.categoryId}" data-txn-amount="${txn.amount}"
                                data-txn-date="${txn.txnDate}" data-txn-payment="${txn.paymentMethod}"
                                data-txn-remark="${fn:escapeXml(txn.remark)}" onclick="editTxn(this)">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <form method="post" action="${ctx}/transactions" class="d-inline"
                              data-confirm="确定删除这笔 ${txn.typeText} ¥${txn.amount} 吗？">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${txn.id}">
                            <input type="hidden" name="back" value="${backPath}">
                            <button class="btn btn-sm btn-ghost text-expense" type="submit" title="删除">
                                <i class="bi bi-trash3"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr><td colspan="7">
                    <div class="empty-state">
                        <i class="bi bi-journal-x"></i>
                        <h3>这个范围内没有收支记录</h3>
                        <p>调整筛选条件，或点击「新增记录」</p>
                    </div>
                </td></tr>
            </c:if>
            </tbody>
        </table>
        <div class="table-hint d-md-none"><i class="bi bi-arrows-expand me-1"></i>左右滑动查看完整表格</div>
    </div>
    <c:if test="${pageData.totalPages > 1}">
        <div class="card-footer pager">
            <div class="meta">第 ${pageData.page} / ${pageData.totalPages} 页 · 每页 ${pageData.size} 条</div>
            <nav>
                <ul class="pagination">
                    <li class="page-item ${pageData.hasPrev ? '' : 'disabled'}">
                        <c:url var="prevUrl" value="/transactions">
                            <c:param name="type" value="${filterType}"/>
                            <c:param name="categoryId" value="${filterCategoryId}"/>
                            <c:param name="start" value="${filterStart}"/>
                            <c:param name="end" value="${filterEnd}"/>
                            <c:param name="keyword" value="${filterKeyword}"/>
                            <c:param name="page" value="${pageData.prevPage}"/>
                        </c:url>
                        <a class="page-link" href="${prevUrl}"><i class="bi bi-chevron-left"></i></a>
                    </li>
                    <c:forEach begin="1" end="${pageData.totalPages}" var="i">
                        <c:if test="${i == 1 || i == pageData.totalPages || (i >= pageData.page - 2 && i <= pageData.page + 2)}">
                            <li class="page-item ${i == pageData.page ? 'active' : ''}">
                                <c:url var="pageUrl" value="/transactions">
                                    <c:param name="type" value="${filterType}"/>
                                    <c:param name="categoryId" value="${filterCategoryId}"/>
                                    <c:param name="start" value="${filterStart}"/>
                                    <c:param name="end" value="${filterEnd}"/>
                                    <c:param name="keyword" value="${filterKeyword}"/>
                                    <c:param name="page" value="${i}"/>
                                </c:url>
                                <a class="page-link" href="${pageUrl}">${i}</a>
                            </li>
                        </c:if>
                    </c:forEach>
                    <li class="page-item ${pageData.hasNext ? '' : 'disabled'}">
                        <c:url var="nextUrl" value="/transactions">
                            <c:param name="type" value="${filterType}"/>
                            <c:param name="categoryId" value="${filterCategoryId}"/>
                            <c:param name="start" value="${filterStart}"/>
                            <c:param name="end" value="${filterEnd}"/>
                            <c:param name="keyword" value="${filterKeyword}"/>
                            <c:param name="page" value="${pageData.nextPage}"/>
                        </c:url>
                        <a class="page-link" href="${nextUrl}"><i class="bi bi-chevron-right"></i></a>
                    </li>
                </ul>
            </nav>
        </div>
    </c:if>
</div>

<div class="modal fade" id="txnModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${ctx}/transactions" id="txnForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="txnModalTitle">记一笔</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="id" id="txnId" value="0">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="back" value="${backPath}">
                    <input type="hidden" name="categoryId" id="txnCategoryId">

                    <div class="mb-3">
                        <label class="form-label d-block">类型</label>
                        <div class="btn-group w-100" role="group">
                            <input type="radio" class="btn-check" name="type" id="typeIncome" value="INCOME">
                            <label class="btn btn-outline-success" for="typeIncome"><i class="bi bi-arrow-down-left me-1"></i>收入</label>
                            <input type="radio" class="btn-check" name="type" id="typeExpense" value="EXPENSE" checked>
                            <label class="btn btn-outline-danger" for="typeExpense"><i class="bi bi-arrow-up-right me-1"></i>支出</label>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label" for="txnCategory">分类</label>
                        <select class="form-select" id="txnCategoryIncome" style="display:none;">
                            <c:forEach items="${incomeCategories}" var="cat">
                                <option value="${cat.id}">${cat.name}</option>
                            </c:forEach>
                        </select>
                        <select class="form-select" id="txnCategoryExpense">
                            <c:forEach items="${expenseCategories}" var="cat">
                                <option value="${cat.id}">${cat.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="row g-3">
                        <div class="col-6">
                            <label class="form-label" for="txnAmount">金额（元）</label>
                            <div class="input-group">
                                <span class="input-group-text">¥</span>
                                <input type="number" class="form-control" id="txnAmount" name="amount"
                                       min="0.01" max="99999999.99" step="0.01" placeholder="0.00" required>
                            </div>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="txnDate">日期</label>
                            <input type="date" class="form-control" id="txnDate" name="txnDate"
                                   value="${today}" max="${today}" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="txnPayment">支付方式</label>
                            <select class="form-select" id="txnPayment" name="paymentMethod">
                                <option>现金</option>
                                <option>微信</option>
                                <option>支付宝</option>
                                <option>银行卡</option>
                                <option>其他</option>
                            </select>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="txnRemark">备注</label>
                            <input type="text" class="form-control" id="txnRemark" name="remark"
                                   maxlength="200" placeholder="例如：下午零售">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>保存记录</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        var modalEl = document.getElementById('txnModal');
        var modal = new bootstrap.Modal(modalEl);
        var form = document.getElementById('txnForm');
        var title = document.getElementById('txnModalTitle');
        var incomeSelect = document.getElementById('txnCategoryIncome');
        var expenseSelect = document.getElementById('txnCategoryExpense');
        var categoryHidden = document.getElementById('txnCategoryId');
        var today = '${today}';

        function currentType() {
            return document.getElementById('typeIncome').checked ? 'INCOME' : 'EXPENSE';
        }

        function applyType(type) {
            document.getElementById('typeIncome').checked = type === 'INCOME';
            document.getElementById('typeExpense').checked = type === 'EXPENSE';
            incomeSelect.style.display = type === 'INCOME' ? '' : 'none';
            expenseSelect.style.display = type === 'EXPENSE' ? '' : 'none';
        }

        window.openTxn = function () {
            form.reset();
            document.getElementById('txnId').value = '0';
            document.getElementById('txnDate').value = today;
            document.getElementById('txnPayment').value = '现金';
            applyType('EXPENSE');
            title.textContent = '记一笔';
            modal.show();
            setTimeout(function () { document.getElementById('txnAmount').focus(); }, 350);
        };

        window.editTxn = function (btn) {
            form.reset();
            var type = btn.getAttribute('data-txn-type');
            document.getElementById('txnId').value = btn.getAttribute('data-txn-id');
            document.getElementById('txnAmount').value = btn.getAttribute('data-txn-amount');
            document.getElementById('txnDate').value = btn.getAttribute('data-txn-date');
            document.getElementById('txnPayment').value = btn.getAttribute('data-txn-payment');
            document.getElementById('txnRemark').value = btn.getAttribute('data-txn-remark');
            applyType(type);
            var target = type === 'INCOME' ? incomeSelect : expenseSelect;
            target.value = btn.getAttribute('data-txn-category');
            title.textContent = '编辑收支记录';
            modal.show();
        };

        document.getElementById('typeIncome').addEventListener('change', function () { applyType('INCOME'); });
        document.getElementById('typeExpense').addEventListener('change', function () { applyType('EXPENSE'); });

        form.addEventListener('submit', function () {
            var select = currentType() === 'INCOME' ? incomeSelect : expenseSelect;
            categoryHidden.value = select.value;
        });

        if (new URLSearchParams(window.location.search).get('open') === '1') {
            window.openTxn();
        }

        document.querySelectorAll('[data-range]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var start = document.querySelector('input[name="start"]');
                var end = document.querySelector('input[name="end"]');
                var now = new Date();
                var fmt = function (d) {
                    return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
                };
                var range = btn.getAttribute('data-range');
                if (range === 'month') {
                    start.value = fmt(new Date(now.getFullYear(), now.getMonth(), 1));
                    end.value = fmt(now);
                } else if (range === 'last-month') {
                    start.value = fmt(new Date(now.getFullYear(), now.getMonth() - 1, 1));
                    end.value = fmt(new Date(now.getFullYear(), now.getMonth(), 0));
                } else if (range === '30') {
                    var d = new Date(now.getTime() - 29 * 86400000);
                    start.value = fmt(d);
                    end.value = fmt(now);
                } else {
                    start.value = fmt(now);
                    end.value = fmt(now);
                }
                start.form.submit();
            });
        });
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
