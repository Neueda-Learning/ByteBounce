<script setup>
import { useRoute } from 'vue-router'
import {
  Bell,
  Clock,
  CreditCard,
  DataAnalysis,
  Monitor,
  Setting,
} from '@element-plus/icons-vue'

const route = useRoute()
</script>

<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <div class="brand">
        <span class="brand-mark">
          <el-icon><Monitor /></el-icon>
        </span>
        <div>
          <div class="brand-title">Transaction Monitoring System</div>
          <div class="brand-subtitle">Risk Operations Console</div>
        </div>
      </div>

      <div class="system-status">
        <span class="status-dot" />
        Monitoring Active
      </div>
    </el-header>

    <el-container class="app-body">
      <el-aside class="app-aside" width="240px">
        <div class="menu-label">Navigation</div>
        <el-menu
          class="app-menu"
          :default-active="route.path"
          background-color="transparent"
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
            <span>Rules</span>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <span class="sidebar-footer-dot" />
          System operational
        </div>
      </el-aside>

      <el-main class="app-main">
        <div class="content-shell">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>
