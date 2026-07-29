<script setup>
import { useRoute } from 'vue-router'
import {
  Bell,
  Clock,
  CreditCard,
  DataAnalysis,
  Lock,
  Setting,
} from '@element-plus/icons-vue'

const route = useRoute()
</script>

<template>
  <el-container class="app-layout">
    <el-aside class="app-aside" width="232px">
      <div class="brand">
        <span class="brand-mark">
          <el-icon><Lock /></el-icon>
        </span>
        <div>
          <div class="brand-title">RiskMonitor</div>
          <div class="brand-subtitle">Monitoring System</div>
        </div>
      </div>

      <div class="menu-label">Risk Operations</div>
      <el-menu
        class="app-menu"
        :default-active="route.path"
        background-color="transparent"
        text-color="#8fa2b7"
        active-text-color="#4aa3ff"
        router
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item index="/transactions">
          <el-icon><CreditCard /></el-icon>
          <span>Transactions</span>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon>
          <span>Alerts</span>
        </el-menu-item>
        <el-menu-item index="/alert-history">
          <el-icon><Clock /></el-icon>
          <span>Alert History</span>
        </el-menu-item>
        <el-menu-item index="/rules">
          <el-icon><Setting /></el-icon>
          <span>Rule Management</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="sidebar-footer-label">System Status</div>
        <div class="sidebar-footer-status">
          <span class="sidebar-footer-dot" />
          Operational
        </div>
        <small>Transaction Monitoring System</small>
      </div>
    </el-aside>

    <el-container class="app-workspace">
      <el-header class="app-header">
        <div>
          <div class="app-context-title">Transaction Monitoring System</div>
          <div class="app-context-subtitle">Financial Risk Operations Console</div>
        </div>
        <div class="system-status">
          <span class="status-dot" />
          Monitoring Active
        </div>
      </el-header>
      <el-main class="app-main">
        <div class="content-shell">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>
