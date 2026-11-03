<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>
<style>
    .cat-manage-row {
        display: flex; align-items: center; gap: 12px; padding: 12px 18px;
        border-bottom: 1px solid var(--line-soft);
    }
    .cat-manage-row:last-child { border-bottom: 0; }
    .cat-manage-row .sort {
        width: 26px; height: 26px; border-radius: 7px; background: #f3f0e7; color: var(--muted);
        display: grid; place-items: center; font-size: 11px; font-weight: 700; flex: 0 0 26px;
    }
    .cat-manage-row .name { font-weight: 600; }
    .cat-manage-row .usage { font-size: 11px; color: var(--muted); }
    .cat-manage-row .actions { margin-left: auto; display: flex; gap: 2px; }
</style>

<div class="row g-3">
    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <span class="stat-icon green" style="width:30px;height:30px;font-size:14px;"><i class="bi bi-arrow-down-left"></i></span>
                <div>
                    <h2>收入分类</h2>
                    <div class="sub">卖货、服务等收入来源</div>
                </div>
                <button class="btn btn-sm btn-outline-secondary ms-auto" type="button" onclick="openCategory('INCOME')">
                    <i class="bi bi-plus-lg me-1"></i>新增
                </button>
            </div>
            <div>
                <c:set var="incomeCount" value="0"/>
                <c:forEach items="${categories}" var="cat">
                    <c:if test="${cat.income}">
                        <c:set var="incomeCount" value="${incomeCount + 1}"/>
                        <div class="cat-manage-row">
                            <span class="sort">${cat.sortOrder}</span>
                            <div>
                                <div class="name">${cat.name}</div>
                                <div class="usage">${cat.transactionCount} 笔收支记录</div>
                            </div>
                            <span class="badge-soft green">收入</span>
                            <div class="actions">
                                <button class="btn btn-sm btn-ghost" type="button" title="编辑"
                                        data-c-id="${cat.id}" data-c-name="${fn:escapeXml(cat.name)}"
                                        data-c-type="${cat.type}" data-c-sort="${cat.sortOrder}"
                                        data-c-count="${cat.transactionCount}" onclick="editCategory(this)">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <c:choose>
                                    <c:when test="${cat.transactionCount > 0}">
                                        <button class="btn btn-sm btn-ghost" type="button" disabled
                                                title="该分类已被使用，无法删除"><i class="bi bi-trash3"></i></button>
                                    </c:when>
                                    <c:otherwise>
                                        <form method="post" action="${ctx}/categories" class="d-inline"
                                              data-confirm="确定删除收入分类「${fn:escapeXml(cat.name)}」吗？">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${cat.id}">
                                            <button class="btn btn-sm btn-ghost text-expense" type="submit" title="删除">
                                                <i class="bi bi-trash3"></i>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>
                <c:if test="${incomeCount == 0}">
                    <div class="empty-state"><i class="bi bi-tag"></i><h3>还没有收入分类</h3><p>新增一个「商品销售」开始吧</p></div>
                </c:if>
            </div>
        </div>
    </div>

    <div class="col-lg-6">
        <div class="card h-100">
            <div class="card-header">
                <span class="stat-icon red" style="width:30px;height:30px;font-size:14px;"><i class="bi bi-arrow-up-right"></i></span>
                <div>
                    <h2>支出分类</h2>
                    <div class="sub">进货、房租、水电等支出</div>
                </div>
                <button class="btn btn-sm btn-outline-secondary ms-auto" type="button" onclick="openCategory('EXPENSE')">
                    <i class="bi bi-plus-lg me-1"></i>新增
                </button>
            </div>
            <div>
                <c:set var="expenseCount" value="0"/>
                <c:forEach items="${categories}" var="cat">
                    <c:if test="${!cat.income}">
                        <c:set var="expenseCount" value="${expenseCount + 1}"/>
                        <div class="cat-manage-row">
                            <span class="sort">${cat.sortOrder}</span>
                            <div>
                                <div class="name">${cat.name}</div>
                                <div class="usage">${cat.transactionCount} 笔收支记录</div>
                            </div>
                            <span class="badge-soft red">支出</span>
                            <div class="actions">
                                <button class="btn btn-sm btn-ghost" type="button" title="编辑"
                                        data-c-id="${cat.id}" data-c-name="${fn:escapeXml(cat.name)}"
                                        data-c-type="${cat.type}" data-c-sort="${cat.sortOrder}"
                                        data-c-count="${cat.transactionCount}" onclick="editCategory(this)">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <c:choose>
                                    <c:when test="${cat.transactionCount > 0}">
                                        <button class="btn btn-sm btn-ghost" type="button" disabled
                                                title="该分类已被使用，无法删除"><i class="bi bi-trash3"></i></button>
                                    </c:when>
                                    <c:otherwise>
                                        <form method="post" action="${ctx}/categories" class="d-inline"
                                              data-confirm="确定删除支出分类「${fn:escapeXml(cat.name)}」吗？">
                                            <input type="hidden" name="action" value="delete">
                                            <input type="hidden" name="id" value="${cat.id}">
                                            <button class="btn btn-sm btn-ghost text-expense" type="submit" title="删除">
                                                <i class="bi bi-trash3"></i>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:if>
                </c:forEach>
                <c:if test="${expenseCount == 0}">
                    <div class="empty-state"><i class="bi bi-tag"></i><h3>还没有支出分类</h3><p>新增一个「进货」开始吧</p></div>
                </c:if>
            </div>
        </div>
    </div>
</div>

<div class="card mt-3">
    <div class="card-body d-flex gap-3 align-items-start">
        <span class="stat-icon amber" style="flex:0 0 36px;"><i class="bi bi-lightbulb"></i></span>
        <div>
            <div class="fw-semibold mb-1">分类使用说明</div>
            <div class="tiny muted" style="line-height:1.8;">
                排序值越小越靠前，用于记账下拉框与报表展示；<br>
                已被收支记录使用的分类不能删除，也不能修改「收入 / 支出」类型，但可以改名与调整排序。
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="categoryModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${ctx}/categories" id="categoryForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="categoryModalTitle">新增分类</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="id" id="categoryId" value="0">
                    <input type="hidden" name="action" value="save">
                    <div class="mb-3">
                        <label class="form-label d-block">分类类型</label>
                        <div class="btn-group w-100" role="group">
                            <input type="radio" class="btn-check" name="type" id="catTypeIncome" value="INCOME">
                            <label class="btn btn-outline-success" for="catTypeIncome"><i class="bi bi-arrow-down-left me-1"></i>收入</label>
                            <input type="radio" class="btn-check" name="type" id="catTypeExpense" value="EXPENSE" checked>
                            <label class="btn btn-outline-danger" for="catTypeExpense"><i class="bi bi-arrow-up-right me-1"></i>支出</label>
                        </div>
                        <div class="form-text" id="typeLockHint" style="display:none;">
                            该分类已被使用，不能修改收支类型。
                        </div>
                    </div>
                    <div class="row g-3">
                        <div class="col-8">
                            <label class="form-label" for="categoryName">分类名称 <span class="text-expense">*</span></label>
                            <input class="form-control" id="categoryName" name="name" maxlength="50" required
                                   placeholder="例如：进货 / 商品销售">
                        </div>
                        <div class="col-4">
                            <label class="form-label" for="categorySort">排序值</label>
                            <input class="form-control" id="categorySort" name="sortOrder" type="number"
                                   min="0" step="1" value="0">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>保存分类</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        var modal = new bootstrap.Modal(document.getElementById('categoryModal'));
        var form = document.getElementById('categoryForm');
        var title = document.getElementById('categoryModalTitle');
        var lockHint = document.getElementById('typeLockHint');
        var radios = [document.getElementById('catTypeIncome'), document.getElementById('catTypeExpense')];

        window.openCategory = function (type) {
            form.reset();
            document.getElementById('categoryId').value = '0';
            document.getElementById('catTypeIncome').checked = type === 'INCOME';
            document.getElementById('catTypeExpense').checked = type === 'EXPENSE';
            document.getElementById('categorySort').value = '0';
            radios.forEach(function (r) { r.disabled = false; });
            lockHint.style.display = 'none';
            title.textContent = '新增分类';
            modal.show();
            setTimeout(function () { document.getElementById('categoryName').focus(); }, 350);
        };

        window.editCategory = function (btn) {
            form.reset();
            var id = btn.getAttribute('data-c-id');
            var type = btn.getAttribute('data-c-type');
            var count = Number(btn.getAttribute('data-c-count'));
            document.getElementById('categoryId').value = id;
            document.getElementById('categoryName').value = btn.getAttribute('data-c-name');
            document.getElementById('categorySort').value = btn.getAttribute('data-c-sort');
            document.getElementById('catTypeIncome').checked = type === 'INCOME';
            document.getElementById('catTypeExpense').checked = type === 'EXPENSE';
            radios.forEach(function (r) { r.disabled = false; });
            lockHint.style.display = count > 0 ? '' : 'none';
            if (count > 0) { lockHint.textContent = '该分类已被 ' + count + ' 笔记录使用，不能修改收支类型。'; }
            title.textContent = '编辑分类';
            modal.show();
        };
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
