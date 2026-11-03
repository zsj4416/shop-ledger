<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="qs" value="${pageContext.request.queryString}"/>
<c:set var="backPath" value="/reminders${empty qs ? '' : '?'}${qs}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>

<div class="d-flex align-items-center gap-2 mb-3 flex-wrap">
    <ul class="nav nav-pills gap-1">
        <li class="nav-item"><a class="nav-link btn-sm ${filterStatus == 'pending' ? 'active' : ''}"
            href="${ctx}/reminders?status=pending">待办</a></li>
        <li class="nav-item"><a class="nav-link btn-sm ${filterStatus == 'done' ? 'active' : ''}"
            href="${ctx}/reminders?status=done">已完成</a></li>
        <li class="nav-item"><a class="nav-link btn-sm ${filterStatus == 'all' ? 'active' : ''}"
            href="${ctx}/reminders?status=all">全部</a></li>
    </ul>
    <button class="btn btn-primary btn-sm ms-auto" type="button" onclick="openReminder()">
        <i class="bi bi-plus-lg me-1"></i>新建提醒
    </button>
</div>

<div class="row g-3">
    <div class="col-xl-8">
        <div class="card">
            <div class="card-header">
                <h2>${filterStatus == 'done' ? '已完成的提醒' : (filterStatus == 'all' ? '全部提醒' : '待办提醒')}</h2>
                <span class="sub">共 ${fn:length(reminders)} 条</span>
            </div>
            <div class="pt-1 pb-1">
                <c:forEach items="${reminders}" var="r">
                    <div class="reminder-item ${r.done ? 'done' : ''}">
                        <form method="post" action="${ctx}/reminders" class="d-inline">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="id" value="${r.id}">
                            <input type="hidden" name="done" value="${r.done ? '0' : '1'}">
                            <input type="hidden" name="back" value="${backPath}">
                            <button class="reminder-check ${r.done ? 'done' : ''}" type="submit" title="${r.done ? '恢复待办' : '标记完成'}">
                                <i class="bi bi-check-lg"></i>
                            </button>
                        </form>
                        <div class="flex-grow-1 min-w-0">
                            <div class="r-title">${r.title}</div>
                            <c:if test="${not empty r.content}"><div class="r-content">${r.content}</div></c:if>
                            <div class="r-meta">
                                <span class="badge-soft ${r.priorityClass}">${r.priorityText}优先</span>
                                <span class="r-due"><i class="bi bi-calendar3 me-1"></i>${r.dueDate}</span>
                                <span class="r-due ${r.overdue ? 'text-expense' : (r.dueToday ? 'text-income' : '')}">${r.deadlineText}</span>
                                <c:if test="${r.overdue}"><span class="badge-soft red">逾期</span></c:if>
                            </div>
                            <c:if test="${not empty r.relatedCategoryName}">
                                <div class="r-related">
                                    <a href="${ctx}/transactions?type=EXPENSE&amp;categoryId=${r.relatedCategoryId}">
                                        <i class="bi bi-receipt me-1"></i>本月${r.relatedCategoryName}已记支出：
                                        ${r.relatedDate} · ¥<fmt:formatNumber value="${r.relatedAmount}" pattern="#,##0.00"/>
                                    </a>
                                </div>
                            </c:if>
                        </div>
                        <div class="hover-actions nowrap">
                            <button class="btn btn-sm btn-ghost" type="button" title="编辑"
                                    data-r-id="${r.id}" data-r-title="${fn:escapeXml(r.title)}"
                                    data-r-content="${fn:escapeXml(r.content)}" data-r-due="${r.dueDate}"
                                    data-r-priority="${r.priority}" onclick="editReminder(this)">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <form method="post" action="${ctx}/reminders" class="d-inline"
                                  data-confirm="确定删除提醒「${fn:escapeXml(r.title)}」吗？">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="id" value="${r.id}">
                                <input type="hidden" name="back" value="${backPath}">
                                <button class="btn btn-sm btn-ghost text-expense" type="submit" title="删除">
                                    <i class="bi bi-trash3"></i>
                                </button>
                            </form>
                        </div>
                    </div>
                </c:forEach>
                <c:if test="${empty reminders}">
                    <div class="empty-state">
                        <i class="bi bi-bell-slash"></i>
                        <h3>${filterStatus == 'done' ? '还没有已完成的提醒' : '没有待办事项'}</h3>
                        <p>新建交租、对账、许可证续期等提醒，到期不再忘</p>
                        <button class="btn btn-primary btn-sm mt-3" type="button" onclick="openReminder()">新建提醒</button>
                    </div>
                </c:if>
            </div>
        </div>
    </div>
    <div class="col-xl-4">
        <div class="card mb-3">
            <div class="card-header">
                <h2>库存预警</h2>
                <span class="badge-soft amber ms-auto">${fn:length(lowStockProducts)} 项</span>
            </div>
            <div class="pt-1 pb-1">
                <c:forEach items="${lowStockProducts}" var="p">
                    <div class="stock-alert-row">
                        <div class="flex-grow-1 min-w-0">
                            <div class="cell-title text-truncate">${p.name}</div>
                            <div class="cell-sub">库存 ${p.stock}${p.unit} · 阈值 ${p.alertThreshold}${p.unit}</div>
                        </div>
                        <a class="btn btn-sm btn-outline-secondary" href="${ctx}/stock?productId=${p.id}&open=in">补货</a>
                    </div>
                </c:forEach>
                <c:if test="${empty lowStockProducts}">
                    <div class="empty-state py-4">
                        <i class="bi bi-check2-circle"></i>
                        <h3>库存健康</h3>
                        <p>没有低于预警阈值的商品</p>
                    </div>
                </c:if>
            </div>
        </div>
        <div class="card">
            <div class="card-header"><h2>提醒小贴士</h2></div>
            <div class="card-body">
                <div class="timeline">
                    <div class="timeline-item">
                        <div class="t-title">固定支出别忘记</div>
                        <div class="t-meta">房租、水电、平台服务费可以设为每月提醒。</div>
                    </div>
                    <div class="timeline-item">
                        <div class="t-title">定期盘点库存</div>
                        <div class="t-meta">每周盘点一次，在「库存流水 → 盘点」中校正实际数量。</div>
                    </div>
                    <div class="timeline-item">
                        <div class="t-title">月底对账</div>
                        <div class="t-meta">用经营报表核对收入与支出，导出一份 CSV 留档。</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="modal fade" id="reminderModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${ctx}/reminders" id="reminderForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="reminderModalTitle">新建提醒</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="id" id="reminderId" value="0">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="back" value="${backPath}">
                    <div class="mb-3">
                        <label class="form-label" for="reminderTitle">提醒标题 <span class="text-expense">*</span></label>
                        <input class="form-control" id="reminderTitle" name="title" maxlength="100" required
                               placeholder="例如：交下月房租">
                    </div>
                    <div class="row g-3 mb-3">
                        <div class="col-7">
                            <label class="form-label" for="reminderDue">提醒日期 <span class="text-expense">*</span></label>
                            <input class="form-control" id="reminderDue" name="dueDate" type="date" value="${today}" required>
                        </div>
                        <div class="col-5">
                            <label class="form-label" for="reminderPriority">优先级</label>
                            <select class="form-select" id="reminderPriority" name="priority">
                                <option value="HIGH">高</option>
                                <option value="MEDIUM" selected>中</option>
                                <option value="LOW">低</option>
                            </select>
                        </div>
                    </div>
                    <div class="mb-1">
                        <label class="form-label" for="reminderContent">备注内容</label>
                        <textarea class="form-control" id="reminderContent" name="content" rows="3" maxlength="500"
                                  placeholder="补充金额、联系人或其他说明（选填）"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>保存提醒</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        var modal = new bootstrap.Modal(document.getElementById('reminderModal'));
        var form = document.getElementById('reminderForm');
        var title = document.getElementById('reminderModalTitle');

        window.openReminder = function () {
            form.reset();
            document.getElementById('reminderId').value = '0';
            document.getElementById('reminderDue').value = '${today}';
            document.getElementById('reminderPriority').value = 'MEDIUM';
            title.textContent = '新建提醒';
            modal.show();
            setTimeout(function () { document.getElementById('reminderTitle').focus(); }, 350);
        };

        window.editReminder = function (btn) {
            form.reset();
            document.getElementById('reminderId').value = btn.getAttribute('data-r-id');
            document.getElementById('reminderTitle').value = btn.getAttribute('data-r-title');
            document.getElementById('reminderContent').value = btn.getAttribute('data-r-content');
            document.getElementById('reminderDue').value = btn.getAttribute('data-r-due');
            document.getElementById('reminderPriority').value = btn.getAttribute('data-r-priority');
            title.textContent = '编辑提醒';
            modal.show();
        };
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
