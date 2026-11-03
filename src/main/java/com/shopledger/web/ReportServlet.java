package com.shopledger.web;

import com.shopledger.service.ReportService;
import com.shopledger.util.WebUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@WebServlet("/reports")
public class ReportServlet extends BaseServlet {

    private final ReportService reportService = new ReportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        LocalDate start = WebUtil.date(request, "start", today.withDayOfMonth(1));
        LocalDate end = WebUtil.date(request, "end", today);

        try {
            if ("csv".equals(WebUtil.str(request, "export"))) {
                String csv = reportService.exportCsv(start, end);
                String fileName = "report_" + start.format(DateTimeFormatter.BASIC_ISO_DATE) + "_"
                        + end.format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
                response.setContentType("text/csv; charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                PrintWriter writer = response.getWriter();
                writer.write('\uFEFF');
                writer.write(csv);
                writer.flush();
                return;
            }

            Map<String, Object> report = reportService.build(start, end);
            request.setAttribute("report", report);
            request.setAttribute("filterStart", start);
            request.setAttribute("filterEnd", end);
            request.setAttribute("today", today);
            render(request, response, "reports", "经营报表", "reports");
        } catch (RuntimeException e) {
            fail(request, response, "/reports", e);
        }
    }
}
