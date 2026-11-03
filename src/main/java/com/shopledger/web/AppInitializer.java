package com.shopledger.web;

import com.shopledger.util.Db;
import com.shopledger.service.SeedData;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class AppInitializer implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppInitializer.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Db.init();
            SeedData seedData = new SeedData();
            seedData.seedIfEmpty();
            sce.getServletContext().setAttribute("appReady", Boolean.TRUE);
            LOG.info("店铺账本初始化完成");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "初始化失败，请检查 MySQL 是否启动以及 db.properties 配置", e);
            sce.getServletContext().setAttribute("appReady", Boolean.FALSE);
            sce.getServletContext().setAttribute("appError", e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Db.shutdown();
    }
}
