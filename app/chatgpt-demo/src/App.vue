<template>
  <el-row :gutter="20">
    <el-col :span="16">
      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column prop="bookingNumber" label="#" />
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="date" label="Date" />
        <el-table-column prop="from" label="From" />
        <el-table-column prop="to" label="To" />
        <el-table-column prop="bookingStatus" label="Status">
          <template #default="scope">
            {{ scope.row.bookingStatus === "CONFIRMED" ? "✅" : "❌" }}
          </template>
        </el-table-column>
        <el-table-column prop="bookingClass" label="Booking class" />
        <el-table-column label="Operations" fixed="right" width="180">
          <template #default="scope">
            <el-button size="small" type="primary"> 更改预定 </el-button>
            <el-button size="小" type="danger"> 退订 </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-col>

    <el-col :span="8" style="background-color: aliceblue">
      <div style="height: 500px; overflow: scroll">
        <el-timeline style="max-width: 100%">
          <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :icon="activity.icon"
              :type="activity.type"
              :color="activity.color"
              :size="activity.size"
              :hollow="activity.hollow"
              :timestamp="activity.timestamp"
          >
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </div>
      <div id="container">
        <div id="chat">
          <el-input
              v-model="msg"
              input-style="width: 100%;height:50px"
              :rows="2"
              type="text"
              placeholder="Please input"
              @keydown.enter="sendMsg()"
          />
          <el-button @click="sendMsg()">发送</el-button>
        </div>
        <!-- 添加查看历史对话记录按钮 -->
        <div id="history-button" style="margin-top: 10px;">
          <el-button
              @click="loadHistory"
              type="primary"
              size="small"
              style="width: 100%"
          >
            查看当前会话历史
          </el-button>
        </div>
        <div id="session-list-button" style="margin-top: 10px;">
          <el-button
              @click="loadSessionList"
              type="primary"
              size="small"
              style="width: 100%"
          >
            查看历史对话记录
          </el-button>
        </div>
      </div>
    </el-col>
  </el-row>

  <!-- 历史对话记录弹窗 -->
  <el-dialog
      v-model="historyDialogVisible"
      title="历史对话记录"
      width="60%"
      height="70%"
      :before-close="handleHistoryDialogClose"
  >
    <div style="height: 400px; overflow: auto;">
      <el-timeline style="max-width: 100%">
        <el-timeline-item
            v-for="(conversation, index) in historyConversations"
            :key="index"
            :timestamp="formatTimestamp(conversation.createdAt)"
            placement="top"
        >
          <el-card>
            <h4>用户: {{ conversation.userMessage }}</h4>
            <p>助手: {{ conversation.aiResponse }}</p>
          </el-card>
        </el-timeline-item>
        <el-timeline-item v-if="historyConversations.length === 0">
          <el-alert title="暂无历史对话记录" type="info" :closable="false" />
        </el-timeline-item>
      </el-timeline>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="historyDialogVisible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 会话列表弹窗 -->
  <el-dialog
      v-model="sessionListDialogVisible"
      title="历史会话列表"
      width="60%"
      :before-close="handleSessionListDialogClose"
  >
    <div style="height: 400px; overflow: auto;">
      <el-table :data="sessionList" style="width: 100%" @row-click="selectSession">
        <el-table-column prop="sessionId" label="会话ID" />
        <el-table-column prop="messageCount" label="消息数量" />
        <el-table-column prop="latestMessage" label="最新消息" />
        <el-table-column prop="createdAt" label="创建时间" :formatter="formatTimestampSimple" />
      </el-table>
      <el-empty v-if="sessionList.length === 0" description="暂无历史会话"></el-empty>
    </div>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="sessionListDialogVisible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script lang="ts">
import { MoreFilled } from "@element-plus/icons-vue";
import { ref, onMounted } from "vue";
import axios from "axios";
import { useRoute, useRouter } from 'vue-router';

export default {
  setup() {
    const route = useRoute();
    const router = useRouter();
    
    // 检查用户是否已登录
    const token = localStorage.getItem('token');
    if (!token) {
      // 如果没有token，重定向到登录页面
      router.push('/');
    }
    
    const activities = ref([
      {
        content: "⭐欢迎来到ie智能日程助手！请问有什么可以帮您的?",
        timestamp:
            new Date().toLocaleDateString() +
            " " +
            new Date().toLocaleTimeString(),
        color: "#0bbd87",
      },
    ]);
    const msg = ref("");
    const tableData = ref([]);
    let count = 2;
    let eventSource;
    // 使用默认的sessionId，实际项目中应该生成唯一的ID
    const sessionId = "session-" + Date.now();

    // 弹窗相关变量
    const historyDialogVisible = ref(false);
    const historyConversations = ref([]);
    const sessionListDialogVisible = ref(false);
    const sessionList = ref([]);

    // 创建带认证头的axios实例
    const apiClient = axios.create();
    apiClient.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    const sendMsg = () => {
      if (eventSource) {
        eventSource.close();
      }

      activities.value.push({
        content: `你:${msg.value}`,
        timestamp:
            new Date().toLocaleDateString() +
            " " +
            new Date().toLocaleTimeString(),
        size: "large",
        type: "primary",
        icon: MoreFilled,
      });

      activities.value.push({
        content: "waiting...",
        timestamp:
            new Date().toLocaleDateString() +
            " " +
            new Date().toLocaleTimeString(),
        color: "#0bbd87",
      });

      // sse: 服务端推送 Server-Sent Events
      // 添加sessionId参数到请求中
      eventSource = new EventSource(
          `/ai/generateStreamAsString?message=${encodeURIComponent(msg.value)}&sessionId=${sessionId}`
      );
      msg.value = "";
      eventSource.onmessage = (event) => {
        if (event.data === "[complete]") {
          count = count + 2;
          eventSource.close();
          getBookings();
          return;
        }
        activities.value[count].content += event.data;
      };
      eventSource.onopen = (event) => {
        activities.value[count].content = "";
      };
    };

    const getBookings = () => {
      apiClient
          .get("/booking/list")
          .then((response) => {
            tableData.value = response.data;
          })
          .catch((error) => {
            console.error(error);
          });
    };

    // 加载历史对话记录
    const loadHistory = () => {
      // 直接加载当前会话的历史记录
      apiClient
          .get(`/api/conversations/session/${sessionId}`)
          .then((response) => {
            console.log("获取到的历史记录:", response.data);
            historyConversations.value = response.data;
            historyDialogVisible.value = true;
          })
          .catch((error) => {
            console.error("获取历史对话记录失败:", error);
            historyConversations.value = [];
            historyDialogVisible.value = true;
          });
    };

    // 加载会话列表
    const loadSessionList = () => {
      // 从当前用户状态中获取userId
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      const userId = user.id || 1;
      
      apiClient
          .get(`/api/conversations/sessions/user/${userId}`)
          .then((response) => {
            console.log("获取到的会话ID列表:", response.data);
            // 获取每个会话的详细信息
            const sessionIds = response.data;
            const sessionPromises = sessionIds.map(sessionId => {
              return apiClient.get(`/api/conversations/session/${sessionId}`)
                  .then(sessionResponse => {
                    const conversations = sessionResponse.data;
                    return {
                      sessionId: sessionId,
                      messageCount: conversations.length,
                      latestMessage: conversations.length > 0 ? 
                          (conversations[0].userMessage || conversations[0].aiResponse) :
                          '无消息',
                      createdAt: conversations.length > 0 ? conversations[0].createdAt : new Date().toISOString()
                    };
                  })
                  .catch(error => {
                    console.error(`获取会话 ${sessionId} 的详情失败:`, error);
                    return {
                      sessionId: sessionId,
                      messageCount: 0,
                      latestMessage: '获取失败',
                      createdAt: new Date().toISOString()
                    };
                  });
            });

            // 等待所有会话详情获取完成
            Promise.all(sessionPromises).then(sessionDetails => {
              sessionList.value = sessionDetails;
              sessionListDialogVisible.value = true;
            });
          })
          .catch((error) => {
            console.error("获取会话列表失败:", error);
            sessionList.value = [];
            sessionListDialogVisible.value = true;
          });
    };

    // 关闭历史记录弹窗
    const handleHistoryDialogClose = (done: () => void) => {
      historyDialogVisible.value = false;
      done();
    };

    // 关闭会话列表弹窗
    const handleSessionListDialogClose = (done: () => void) => {
      sessionListDialogVisible.value = false;
      done();
    };

    // 格式化时间戳
    const formatTimestamp = (timestamp: string) => {
      if (!timestamp) return '';
      return new Date(timestamp).toLocaleDateString() +
          " " +
          new Date(timestamp).toLocaleTimeString();
    };

    // 简单时间戳格式化
    const formatTimestampSimple = (row, column, cellValue) => {
      if (!cellValue) return '';
      return new Date(cellValue).toLocaleDateString() +
          " " +
          new Date(cellValue).toLocaleTimeString();
    };

    onMounted(() => {
      getBookings();
    });

    // 选择会话
    const selectSession = (row) => {
      console.log("选择的会话:", row);
      // 加载选中会话的详细对话记录
      apiClient
          .get(`/api/conversations/session/${row.sessionId}`)
          .then((response) => {
            console.log("获取到的会话详情:", response.data);
            historyConversations.value = response.data;
            sessionListDialogVisible.value = false;
            historyDialogVisible.value = true;
          })
          .catch((error) => {
            console.error("获取会话详情失败:", error);
            historyConversations.value = [];
            sessionListDialogVisible.value = false;
            historyDialogVisible.value = true;
          });
    };

    return {
      activities,
      msg,
      tableData,
      sendMsg,
      getBookings,
      loadHistory,
      loadSessionList,
      historyDialogVisible,
      historyConversations,
      sessionListDialogVisible,
      sessionList,
      selectSession,
      handleHistoryDialogClose,
      handleSessionListDialogClose,
      formatTimestamp,
      formatTimestampSimple
    };
  },
};
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
}
#chat button {
  position: absolute;
  margin-left: -60px;
  margin-top: 19px;
}
</style>