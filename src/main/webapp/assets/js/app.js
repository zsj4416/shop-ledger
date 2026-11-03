(function () {
    'use strict';

    // 页面内容带 fadeUp 动画，transform 会形成 stacking context，
    // 导致 Bootstrap 的 .modal-backdrop 盖在弹窗上方（弹窗可见但点不动）。
    // 在 Bootstrap 初始化之前把弹窗提升到 body 直接子节点即可避免。
    document.querySelectorAll('.modal').forEach(function (modal) {
        document.body.appendChild(modal);
    });

    function ready(fn) {
        if (document.readyState !== 'loading') {
            fn();
        } else {
            document.addEventListener('DOMContentLoaded', fn);
        }
    }

    ready(function () {
        // 顶栏日期
        var dateEl = document.getElementById('topbarDate');
        var weekEl = document.getElementById('topbarWeekday');
        if (dateEl) {
            var now = new Date();
            dateEl.textContent = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' });
            if (weekEl) {
                weekEl.textContent = now.toLocaleDateString('zh-CN', { weekday: 'long' })
                    + ' · 经营愉快';
            }
        }

        // 移动端侧边栏
        var sidebar = document.getElementById('sidebar');
        var overlay = document.getElementById('sidebarOverlay');
        var toggles = document.querySelectorAll('[data-sidebar-toggle]');
        toggles.forEach(function (btn) {
            btn.addEventListener('click', function () {
                if (sidebar) { sidebar.classList.toggle('show'); }
                if (overlay) { overlay.classList.toggle('show'); }
            });
        });
        if (overlay) {
            overlay.addEventListener('click', function () {
                sidebar.classList.remove('show');
                overlay.classList.remove('show');
            });
        }

        // 自动淡出提示
        document.querySelectorAll('.alert-auto').forEach(function (el) {
            setTimeout(function () {
                el.style.transition = 'opacity .4s ease, transform .4s ease';
                el.style.opacity = '0';
                el.style.transform = 'translateY(-4px)';
                setTimeout(function () { el.remove(); }, 420);
            }, 4800);
        });

        // 删除类操作确认
        document.querySelectorAll('form[data-confirm]').forEach(function (form) {
            form.addEventListener('submit', function (e) {
                if (!window.confirm(form.getAttribute('data-confirm'))) {
                    e.preventDefault();
                }
            });
        });

        // 数字输入自动全选
        document.querySelectorAll('input[type="number"]').forEach(function (input) {
            input.addEventListener('focus', function () { input.select(); });
        });

        // Bootstrap tooltip
        if (window.bootstrap) {
            document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
                new bootstrap.Tooltip(el);
            });
        }
    });
})();
