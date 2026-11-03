<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="qs" value="${pageContext.request.queryString}"/>
<c:set var="backPath" value="/products${empty qs ? '' : '?'}${qs}"/>
<jsp:include page="/WEB-INF/views/common/head.jsp"/>

<div class="filter-bar">
    <form method="get" action="${ctx}/products" class="row g-2 align-items-end">
        <div class="col-12 col-md-4">
            <label class="form-label">搜索商品</label>
            <input class="form-control" type="search" name="keyword" value="${filterKeyword}"
                   placeholder="商品名称或商品编码">
        </div>
        <div class="col-6 col-md-3">
            <label class="form-label">商品分类</label>
            <select class="form-select" name="category">
                <option value="">全部分类</option>
                <c:forEach items="${productCategories}" var="cat">
                    <option value="${cat}" ${cat == filterCategory ? 'selected' : ''}>${cat}</option>
                </c:forEach>
            </select>
        </div>
        <div class="col-6 col-md-2">
            <label class="form-label d-block">&nbsp;</label>
            <div class="form-check">
                <input class="form-check-input" type="checkbox" name="low" value="1" id="lowOnly" ${lowStockOnly ? 'checked' : ''}>
                <label class="form-check-label tiny" for="lowOnly">仅看库存预警</label>
            </div>
        </div>
        <div class="col-12 col-md-3 d-flex gap-2">
            <button class="btn btn-dark btn-sm" type="submit"><i class="bi bi-search me-1"></i>查询</button>
            <a class="btn btn-outline-secondary btn-sm" href="${ctx}/products">重置</a>
            <button class="btn btn-primary btn-sm ms-auto" type="button" onclick="openProduct()">
                <i class="bi bi-plus-lg me-1"></i>新增商品
            </button>
        </div>
    </form>
</div>

<div class="row g-2 mb-3">
    <div class="col-md-3 col-6">
        <div class="card"><div class="card-body py-3">
            <div class="tiny muted">筛选结果</div>
            <div class="fs-5 fw-bold">${pageData.total} <small class="tiny muted fw-normal">种商品</small></div>
        </div></div>
    </div>
    <div class="col-md-3 col-6">
        <div class="card"><div class="card-body py-3">
            <div class="tiny muted">库存预警</div>
            <div class="fs-5 fw-bold ${lowStockCount > 0 ? 'text-expense' : 'text-income'}">
                ${lowStockCount} <small class="tiny muted fw-normal">项待补货</small>
            </div>
        </div></div>
    </div>
    <div class="col-md-6">
        <div class="card"><div class="card-body py-3 d-flex align-items-center justify-content-between">
            <div>
                <div class="tiny muted">库存提示</div>
                <div class="tiny">低于预警阈值的商品会标红，可一键去补货</div>
            </div>
            <a class="btn btn-sm btn-outline-secondary" href="${ctx}/stock">
                <i class="bi bi-arrow-left-right me-1"></i>库存流水
            </a>
        </div></div>
    </div>
</div>

<div class="card">
    <div class="card-header">
        <h2>商品档案</h2>
        <span class="sub">点击行尾按钮可编辑、补货或停用</span>
    </div>
    <div class="table-responsive">
        <table class="table align-middle">
            <thead>
            <tr>
                <th>商品</th>
                <th>分类</th>
                <th class="text-end">进价</th>
                <th class="text-end">售价</th>
                <th class="text-end">单件毛利</th>
                <th class="text-end">当前库存</th>
                <th>状态</th>
                <th class="text-end" style="width:170px;">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${pageData.items}" var="p">
                <tr>
                    <td>
                        <div class="cell-title">${p.name}</div>
                        <div class="cell-sub">${p.sku}</div>
                    </td>
                    <td><span class="badge-soft gray">${p.category}</span></td>
                    <td class="text-end">¥<fmt:formatNumber value="${p.costPrice}" pattern="#,##0.00"/></td>
                    <td class="text-end">¥<fmt:formatNumber value="${p.salePrice}" pattern="#,##0.00"/></td>
                    <td class="text-end text-income">¥<fmt:formatNumber value="${p.grossMargin}" pattern="#,##0.00"/></td>
                    <td class="text-end">
                        <span class="amount ${p.outOfStock ? 'expense' : (p.lowStock ? 'text-warning' : '')}">
                            ${p.stock}<small class="fw-normal text-muted">${p.unit}</small>
                        </span>
                        <c:if test="${p.lowStock}">
                            <div><span class="badge-soft ${p.outOfStock ? 'red' : 'amber'}">
                                <i class="bi bi-exclamation-triangle"></i>${p.outOfStock ? '缺货' : '偏低'}
                            </span></div>
                        </c:if>
                    </td>
                    <td>
                        <span class="badge-soft ${p.active ? 'green' : 'gray'}">${p.active ? '在售' : '停用'}</span>
                    </td>
                    <td class="text-end hover-actions nowrap">
                        <a class="btn btn-sm btn-ghost" title="入库" href="${ctx}/stock?productId=${p.id}&open=in">
                            <i class="bi bi-box-arrow-in-down"></i>
                        </a>
                        <a class="btn btn-sm btn-ghost" title="出库" href="${ctx}/stock?productId=${p.id}&open=out">
                            <i class="bi bi-box-arrow-up"></i>
                        </a>
                        <button class="btn btn-sm btn-ghost" type="button" title="编辑"
                                data-p-id="${p.id}" data-p-sku="${fn:escapeXml(p.sku)}"
                                data-p-name="${fn:escapeXml(p.name)}" data-p-category="${fn:escapeXml(p.category)}"
                                data-p-unit="${fn:escapeXml(p.unit)}" data-p-cost="${p.costPrice}"
                                data-p-sale="${p.salePrice}" data-p-threshold="${p.alertThreshold}"
                                onclick="editProduct(this)">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <form method="post" action="${ctx}/products" class="d-inline"
                              data-confirm="确定${p.active ? '停用' : '启用'}「${fn:escapeXml(p.name)}」吗？">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="id" value="${p.id}">
                            <input type="hidden" name="back" value="${backPath}">
                            <button class="btn btn-sm btn-ghost" type="submit" title="${p.active ? '停用' : '启用'}">
                                <i class="bi ${p.active ? 'bi-pause-circle' : 'bi-play-circle'}"></i>
                            </button>
                        </form>
                        <form method="post" action="${ctx}/products" class="d-inline"
                              data-confirm="确定删除「${fn:escapeXml(p.name)}」吗？已有库存流水的商品不能删除。">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${p.id}">
                            <input type="hidden" name="back" value="${backPath}">
                            <button class="btn btn-sm btn-ghost text-expense" type="submit" title="删除">
                                <i class="bi bi-trash3"></i>
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty pageData.items}">
                <tr><td colspan="8">
                    <div class="empty-state">
                        <i class="bi bi-box"></i>
                        <h3>没有找到商品</h3>
                        <p>换个关键词，或点击「新增商品」建立第一件商品</p>
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
                    <c:url var="prevUrl" value="/products">
                        <c:param name="keyword" value="${filterKeyword}"/>
                        <c:param name="category" value="${filterCategory}"/>
                        <c:param name="low" value="${lowStockOnly ? '1' : ''}"/>
                        <c:param name="page" value="${pageData.prevPage}"/>
                    </c:url>
                    <a class="page-link" href="${prevUrl}"><i class="bi bi-chevron-left"></i></a>
                </li>
                <c:forEach begin="1" end="${pageData.totalPages}" var="i">
                    <c:if test="${i == 1 || i == pageData.totalPages || (i >= pageData.page - 2 && i <= pageData.page + 2)}">
                        <li class="page-item ${i == pageData.page ? 'active' : ''}">
                            <c:url var="pageUrl" value="/products">
                                <c:param name="keyword" value="${filterKeyword}"/>
                                <c:param name="category" value="${filterCategory}"/>
                                <c:param name="low" value="${lowStockOnly ? '1' : ''}"/>
                                <c:param name="page" value="${i}"/>
                            </c:url>
                            <a class="page-link" href="${pageUrl}">${i}</a>
                        </li>
                    </c:if>
                </c:forEach>
                <li class="page-item ${pageData.hasNext ? '' : 'disabled'}">
                    <c:url var="nextUrl" value="/products">
                        <c:param name="keyword" value="${filterKeyword}"/>
                        <c:param name="category" value="${filterCategory}"/>
                        <c:param name="low" value="${lowStockOnly ? '1' : ''}"/>
                        <c:param name="page" value="${pageData.nextPage}"/>
                    </c:url>
                    <a class="page-link" href="${nextUrl}"><i class="bi bi-chevron-right"></i></a>
                </li>
            </ul></nav>
        </div>
    </c:if>
</div>

<div class="modal fade" id="productModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
            <form method="post" action="${ctx}/products" id="productForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="productModalTitle">新增商品</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="id" id="productId" value="0">
                    <input type="hidden" name="action" value="save">
                    <input type="hidden" name="back" value="${backPath}">
                    <div class="row g-3">
                        <div class="col-md-7">
                            <label class="form-label" for="productName">商品名称 <span class="text-expense">*</span></label>
                            <input class="form-control" id="productName" name="name" maxlength="100" required
                                   placeholder="例如：冰镇可乐 330ml">
                        </div>
                        <div class="col-md-5">
                            <label class="form-label" for="productSku">商品编码</label>
                            <input class="form-control" id="productSku" name="sku" maxlength="40"
                                   placeholder="留空自动生成">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="productCategory">商品分类 <span class="text-expense">*</span></label>
                            <input class="form-control" id="productCategory" name="category" list="categoryOptions"
                                   maxlength="50" required placeholder="例如：饮料">
                            <datalist id="categoryOptions">
                                <c:forEach items="${productCategories}" var="cat"><option value="${cat}"></option></c:forEach>
                            </datalist>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="productUnit">单位</label>
                            <input class="form-control" id="productUnit" name="unit" maxlength="10" value="件">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="productThreshold">库存预警阈值</label>
                            <input class="form-control" id="productThreshold" name="alertThreshold" type="number"
                                   min="0" step="1" value="5">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="productCost">进价（元）</label>
                            <div class="input-group">
                                <span class="input-group-text">¥</span>
                                <input class="form-control" id="productCost" name="costPrice" type="number"
                                       min="0" step="0.01" value="0.00">
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label" for="productSale">售价（元）</label>
                            <div class="input-group">
                                <span class="input-group-text">¥</span>
                                <input class="form-control" id="productSale" name="salePrice" type="number"
                                       min="0" step="0.01" value="0.00">
                            </div>
                        </div>
                        <div class="col-md-4" id="initialStockGroup">
                            <label class="form-label" for="productStock">初始库存</label>
                            <input class="form-control" id="productStock" name="stock" type="number"
                                   min="0" step="1" value="0">
                            <div class="form-text">大于 0 时会自动生成一笔期初入库流水</div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check2 me-1"></i>保存商品</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/foot.jsp"/>
<script>
    (function () {
        var modal = new bootstrap.Modal(document.getElementById('productModal'));
        var form = document.getElementById('productForm');
        var stockGroup = document.getElementById('initialStockGroup');
        var title = document.getElementById('productModalTitle');

        window.openProduct = function () {
            form.reset();
            document.getElementById('productId').value = '0';
            document.getElementById('productUnit').value = '件';
            document.getElementById('productThreshold').value = '5';
            document.getElementById('productCost').value = '0.00';
            document.getElementById('productSale').value = '0.00';
            document.getElementById('productStock').value = '0';
            stockGroup.style.display = '';
            title.textContent = '新增商品';
            modal.show();
            setTimeout(function () { document.getElementById('productName').focus(); }, 350);
        };

        window.editProduct = function (btn) {
            form.reset();
            document.getElementById('productId').value = btn.getAttribute('data-p-id');
            document.getElementById('productSku').value = btn.getAttribute('data-p-sku');
            document.getElementById('productName').value = btn.getAttribute('data-p-name');
            document.getElementById('productCategory').value = btn.getAttribute('data-p-category');
            document.getElementById('productUnit').value = btn.getAttribute('data-p-unit');
            document.getElementById('productCost').value = btn.getAttribute('data-p-cost');
            document.getElementById('productSale').value = btn.getAttribute('data-p-sale');
            document.getElementById('productThreshold').value = btn.getAttribute('data-p-threshold');
            stockGroup.style.display = 'none';
            title.textContent = '编辑商品';
            modal.show();
        };

        if (new URLSearchParams(window.location.search).get('open') === '1') {
            window.openProduct();
        }
    })();
</script>
<jsp:include page="/WEB-INF/views/common/end.jsp"/>
