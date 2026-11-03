<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="qs" value="${pageContext.request.queryString}"/>
<c:set var="backPath" value="/stock${empty qs ? '' : '?'}${qs}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>

<div class="row g-2 mb-3">
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top">
                <span class="stat-label">入库</span>
                <span class="stat-icon green"><i class="bi bi-box-arrow-in-down"></i></span>
            </div>
            <div class="stat-value">${summary.inQuantity}<small> 件</small></div>
            <div class="stat-foot">金额 ¥<fmt:formatNumber value="${summary.inAmount}" pattern="#,##0.00"/></div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top">
                <span class="stat-label">出库</span>
                <span class="stat-icon red"><i class="bi bi-box-arrow-up"></i></span>
            </div>
            <div class="stat-value">${summary.outQuantity}<small> 件</small></div>
            <div class="stat-foot">金额 ¥<fmt:formatNumber value="${summary.outAmount}" pattern="#,##0.00"/></div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top">
                <span class="stat-label">库存成本价值</span>
                <span class="stat-icon ink"><i class="bi bi-box-seam"></i></span>
            </div>
            <div class="stat-value">¥ <fmt:formatNumber value="${summary.stockValue}" pattern="#,##0"/></div>
            <div class="stat-foot">${summary.productCount} 种商品</div>
        </div>
    </div>
    <div class="col-md-3 col-6">
        <div class="stat-card">
            <div class="stat-top">
                <span class="stat-label">库存预警</span>
                <span class="stat-icon amber"><i class="bi bi-exclamation-triangle"></i></span>
            </div>
            <div class="stat-value">${summary.lowStockCount}<small> 项</small></div>
            <div class="stat-foot">
                <c:choose>
                    <c:when test="${summary.lowStockCount > 0}"><span class="down">建议尽快补货</span></c:when>
                    <c:otherwise><span class="up">库存健康</span></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<div class="filter-bar">
    <form method="get" action="${ctx}/stock" class="row g-2 align-items-end">
        <div class="col-12 col-md-4">
            <label class="form-label">商品</label>
            <select class="form-select" name="productId">
                <option value="">全部商品</option>
                <c:forEach items="${products}" var="p">
                    <option value="${p.id}" ${p.id == filterProductId ? 'selected' : ''}>${p.name}（库存 ${p.stock}${p.unit}）</option>
                </c:forEach>
            </select>
        </div>
        <div class="col-6 col-md-2">
            <label class="form-label">类型</label>
            <select class="form-select" name="type">
                <option value="">全部类型</option>
                <option value="IN" ${filterType == 'IN' ? 'selected' : ''}>入库</option>
                <option value="OUT" ${filterType == 'OUT' ? 'selected' : ''}>出库</option>
                <option value="ADJUST" ${filterType == 'ADJUST' ? 'selected' : ''}>盘点调整</option>
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
        <div class="col-6 col-md-2 d-flex gap-2">
            <button class="btn btn-dark btn-sm" type="submit"><i class="bi bi-search me-1"></i>查询</button>
            <a class="btn btn-outline-secondary btn-sm" href="${ctx}/stock">重置</a>
        </div>
        <div class="col-12 d-flex gap-2 flex-wrap mt-2">
            <button class="btn btn-primary btn-sm" type="button" onclick="openStock('IN')">
                <i class="bi bi-box-arrow-in-down me-1"></i>入库登记
            </button>
            <button class="btn btn-outline-danger btn-sm" type="button" onclick="openStock('OUT')">
                <i class="bi bi-box-arrow-up me-1"></i>出库登记
            </button>
            <button class="btn btn-outline-secondary btn-sm" type="button" onclick="openStock('ADJUST')">
                <i class="bi bi-clipboard-check me-1"></i>库存盘点
            </button>
            <c:if test="${not empty selectedProduct}">
                <span class="tiny muted ms-auto align-self-center">
                    当前查看：${selectedProduct.name} · 库存 ${selectedProduct.stock}${selectedProduct.unit}
                </span>
            </c:if>
        </div>
    </form>
</div>

<div class="card">
    <div class="card-header">
        <h2>库存流水</h2>
        <span class="sub">共 ${pageData.total} 条记录</span>
    </div>
    <div class="table-responsive">
        <table class="table align-middle">
            <thead>
            <tr>
                <th style="width:105px;">日期</th>
                <th>商品</th>
                <th>类型</th>
                <th class="text-end">数量</th>
                <th class="text-end">单价</th>
                <th class="text-end">金额</th>
                <th>备注</th>
                <th>操作人</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${pageData.items}" var="m">
                <tr>
                    <td class="nowrap">${m.occurredAt}</td>
                    <td>
                        <div class="cell-title">${m.productName}</div>
                        <div class="cell-sub">${m.productSku}</div>
                    </td>
                    <td>
                        <span class="badge-soft ${m.type == 'IN' ? 'green' : (m.type == 'OUT' ? 'red' : 'amber')}">
                            <span class="type-dot ${m.type == 'IN' ? 'in' : (m.type == 'OUT' ? 'out' : 'adjust')}"></span>
                            ${m.typeText}
                        </span>
                        <c:if test="${not empty m.transactionId}">
                            <div class="tiny"><a href="${ctx}/transactions?keyword=${m.productName}">
                                <c:choose><c:when test="${m.type == 'OUT'}">已记收入</c:when><c:otherwise>已记支出</c:otherwise></c:choose>
                            </a></div>
                        </c:if>
                    </td>
                    <td class="text-end amount ${m.signedQuantity > 0 ? 'income' : (m.signedQuantity < 0 ? 'expense' : '')}">
                        <c:if test="${m.signedQuantity > 0}">+</c:if>${m.signedQuantity}<small class="fw-normal text-muted">${m.productUnit}</small>
                    </td>
                    <td class="text-end">¥<fmt:formatNumber value="${m.unitPrice}" pattern="#,##0.00"/></td>
                    <td class="text-end">
                        <c:choose>
                            <c:when test="${m.type == 'ADJUST'}"><span class="muted">—</span></c:when>
                            <c:otherwise>¥<fmt:formatNumber value="${m.amount}" pattern="#,##0.00"/></c:otherwise>
                        </c:choose>
                    </td>
                    <td class="tiny muted" style="max-width:240px;">${empty m.note ? '—' : m.note}</td>
                    <td class="tiny muted">${m.operatorName}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr><td colspan="8">
                    <div class="empty-state">
                        <i class="bi bi-arrow-left-right"></i>
                        <h3>没有库存流水</h3>
                        <p>点击「入库登记」记录第一笔进货</p>
                    </div>
                </td></tr>
            </c:if>
            </tbody>
        </table>
        <div class="table-hint d-md-none"><i class="bi bi-arrows-expand me-1"></i>左右滑动查看完整表格</div>
    </div>
    <c:if test="${pageData.totalPages > 1}">
        <div class="card-footer pager">
            <div class="meta">第 ${pageData.page} / ${pageData.totalPages} 页</div>
            <nav><ul class="pagination">
                <li class="page-item ${pageData.hasPrev ? '' : 'disabled'}">
                    <c:url var="prevUrl" value="/stock">
                        <c:param name="productId" value="${filterProductId > 0 ? filterProductId : ''}"/>
                        <c:param name="type" value="${filterType}"/>
                        <c:param name="start" value="${filterStart}"/>
                        <c:param name="end" value="${filterEnd}"/>
                        <c:param name="page" value="${pageData.prevPage}"/>
                    </c:url>
                    <a class="page-link" href="${prevUrl}"><i class="bi bi-chevron-left"></i></a>
                </li>
                <c:forEach begin="1" end="${pageData.totalPages}" var="i">
                    <c:if test="${i == 1 || i == pageData.totalPages || (i >= pageData.page - 2 && i <= pageData.page + 2)}">
                        <li class="page-item ${i == pageData.page ? 'active' : ''}">
                            <c:url var="pageUrl" value="/stock">
                                <c:param name="productId" value="${filterProductId > 0 ? filterProductId : ''}"/>
                                <c:param name="type" value="${filterType}"/>
                                <c:param name="start" value="${filterStart}"/>
                                <c:param name="end" value="${filterEnd}"/>
                                <c:param name="page" value="${i}"/>
                            </c:url>
                            <a class="page-link" href="${pageUrl}">${i}</a>
                        </li>
                    </c:if>
                </c:forEach>
                <li class="page-item ${pageData.hasNext ? '' : 'disabled'}">
                    <c:url var="nextUrl" value="/stock">
                        <c:param name="productId" value="${filterProductId > 0 ? filterProductId : ''}"/>
                        <c:param name="type" value="${filterType}"/>
                        <c:param name="start" value="${filterStart}"/>
                        <c:param name="end" value="${filterEnd}"/>
                        <c:param name="page" value="${pageData.nextPage}"/>
                    </c:url>
                    <a class="page-link" href="${nextUrl}"><i class="bi bi-chevron-right"></i></a>
                </li>
            </ul></nav>
        </div>
    </c:if>
</div>

<div class="modal fade" id="stockModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${ctx}/stock" id="stockForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="stockModalTitle">入库登记</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="back" value="${backPath}">
                    <div class="mb-3">
                        <label class="form-label d-block">操作类型</label>
                        <div class="btn-group w-100" role="group">
                            <input type="radio" class="btn-check" name="type" id="stockTypeIn" value="IN" checked>
                            <label class="btn btn-outline-success" for="stockTypeIn"><i class="bi bi-box-arrow-in-down me-1"></i>入库</label>
                            <input type="radio" class="btn-check" name="type" id="stockTypeOut" value="OUT">
                            <label class="btn btn-outline-danger" for="stockTypeOut"><i class="bi bi-box-arrow-up me-1"></i>出库</label>
                            <input type="radio" class="btn-check" name="type" id="stockTypeAdjust" value="ADJUST">
                            <label class="btn btn-outline-secondary" for="stockTypeAdjust"><i class="bi bi-clipboard-check me-1"></i>盘点</label>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label" for="stockProduct">商品 <span class="text-expense">*</span></label>
                        <select class="form-select" id="stockProduct" name="productId" required>
                            <option value="">请选择商品</option>
                            <c:forEach items="${products}" var="p">
                                <option value="${p.id}" data-stock="${p.stock}" data-unit="${fn:escapeXml(p.unit)}"
                                        data-cost="${p.costPrice}" data-sale="${p.salePrice}"
                                        data-name="${fn:escapeXml(p.name)}">${p.name}（${p.sku}）</option>
                            </c:forEach>
                        </select>
                        <div class="form-text" id="stockHint">选择商品后显示当前库存</div>
                    </div>
                    <div class="row g-3">
                        <div class="col-6">
                            <label class="form-label" for="stockQuantity" id="quantityLabel">入库数量</label>
                            <div class="input-group">
                                <input class="form-control" id="stockQuantity" name="quantity" type="number"
                                       min="1" step="1" value="1" required>
                                <span class="input-group-text" id="quantityUnit">件</span>
                            </div>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="stockPrice">单价（元）</label>
                            <div class="input-group">
                                <span class="input-group-text">¥</span>
                                <input class="form-control" id="stockPrice" name="unitPrice" type="number"
                                       min="0" step="0.01" placeholder="默认按商品价格">
                            </div>
                        </div>
                        <div class="col-6">
                            <label class="form-label" for="stockDate">业务日期</label>
                            <input class="form-control" id="stockDate" name="occurredAt" type="date"
                                   value="${today}" max="${today}" required>
                        </div>
                        <div class="col-6" id="syncIncomeGroup" style="display:none;">
                            <label class="form-label d-block">&nbsp;</label>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="syncIncome" value="1" id="syncIncome">
                                <label class="form-check-label" for="syncIncome">同时记一笔销售收入</label>
                            </div>
                            <div class="form-text">按出库单价 × 数量生成收入</div>
                        </div>
                        <div class="col-6" id="syncExpenseGroup">
                            <label class="form-label d-block">&nbsp;</label>
                            <div class="form-check">
                                <input class="form-check-input" type="checkbox" name="syncExpense" value="1" id="syncExpense" checked>
                                <label class="form-check-label" for="syncExpense">同时记一笔进货支出</label>
                            </div>
                            <div class="form-text">按进价 × 数量计入今日支出</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label" for="stockNote">备注</label>
                            <input class="form-control" id="stockNote" name="note" maxlength="200"
                                   placeholder="例如：供应商补货 / 门店零售">
                        </div>
                    </div>
                    <div class="alert alert-warning mt-3 mb-0 py-2" id="adjustHint" style="display:none;">
                        <i class="bi bi-info-circle me-1"></i>盘点请填写盘点后的<strong>实际库存数量</strong>，系统会自动计算盈亏。
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>确认提交</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        var modal = new bootstrap.Modal(document.getElementById('stockModal'));
        var form = document.getElementById('stockForm');
        var productSelect = document.getElementById('stockProduct');
        var hint = document.getElementById('stockHint');
        var priceInput = document.getElementById('stockPrice');
        var quantity = document.getElementById('stockQuantity');
        var syncGroup = document.getElementById('syncIncomeGroup');
        var expenseGroup = document.getElementById('syncExpenseGroup');
        var adjustHint = document.getElementById('adjustHint');
        var today = '${today}';

        function currentType() {
            if (document.getElementById('stockTypeOut').checked) { return 'OUT'; }
            if (document.getElementById('stockTypeAdjust').checked) { return 'ADJUST'; }
            return 'IN';
        }

        function applyType(type) {
            document.getElementById('stockTypeIn').checked = type === 'IN';
            document.getElementById('stockTypeOut').checked = type === 'OUT';
            document.getElementById('stockTypeAdjust').checked = type === 'ADJUST';
            var titles = { IN: '入库登记', OUT: '出库登记', ADJUST: '库存盘点' };
            document.getElementById('stockModalTitle').textContent = titles[type];
            document.getElementById('quantityLabel').textContent =
                type === 'IN' ? '入库数量' : (type === 'OUT' ? '出库数量' : '盘点后实际库存');
            quantity.min = type === 'ADJUST' ? '0' : '1';
            syncGroup.style.display = type === 'OUT' ? '' : 'none';
            expenseGroup.style.display = type === 'IN' ? '' : 'none';
            adjustHint.style.display = type === 'ADJUST' ? '' : 'none';
            priceInput.closest('.col-6').style.display = type === 'ADJUST' ? 'none' : '';
            updateHint();
        }

        function updateHint() {
            var opt = productSelect.options[productSelect.selectedIndex];
            if (!opt || !opt.value) {
                hint.textContent = '选择商品后显示当前库存';
                priceInput.placeholder = '默认按商品价格';
                document.getElementById('quantityUnit').textContent = '件';
                return;
            }
            var stock = opt.getAttribute('data-stock');
            var unit = opt.getAttribute('data-unit');
            var cost = Number(opt.getAttribute('data-cost')).toFixed(2);
            var sale = Number(opt.getAttribute('data-sale')).toFixed(2);
            var type = currentType();
            hint.textContent = '当前库存 ' + stock + unit + ' · 进价 ¥' + cost + ' · 售价 ¥' + sale;
            priceInput.placeholder = type === 'IN' ? ('默认进价 ¥' + cost) : ('默认售价 ¥' + sale);
            document.getElementById('quantityUnit').textContent = unit;
            if (type === 'ADJUST') {
                quantity.value = stock;
            }
        }

        window.openStock = function (type, productId) {
            form.reset();
            document.getElementById('stockDate').value = today;
            quantity.value = '1';
            applyType(type || 'IN');
            if (productId) { productSelect.value = String(productId); }
            updateHint();
            modal.show();
            setTimeout(function () { productSelect.focus(); }, 350);
        };

        document.getElementById('stockTypeIn').addEventListener('change', function () { applyType('IN'); });
        document.getElementById('stockTypeOut').addEventListener('change', function () { applyType('OUT'); });
        document.getElementById('stockTypeAdjust').addEventListener('change', function () { applyType('ADJUST'); });
        productSelect.addEventListener('change', updateHint);

        var open = '${openModal}';
        if (open) {
            var type = open === 'out' ? 'OUT' : (open === 'adjust' ? 'ADJUST' : 'IN');
            window.openStock(type, '${filterProductId > 0 ? filterProductId : ""}');
        }
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
