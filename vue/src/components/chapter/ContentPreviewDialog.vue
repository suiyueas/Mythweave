<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div class="modal-overlay" @click.self="$emit('close')">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ chapter?.title || '章节内容' }}</h2>
            <button class="btn-close" @click="$emit('close')">✕</button>
          </div>
          <div class="content-body">
            <div v-if="chapter?.content" class="text-content">{{ chapter.content || '' }}</div>
            <div v-else class="empty-content">（该章节暂无内容）</div>
          </div>
          <div class="modal-footer">
            <button class="btn-cancel" @click="$emit('close')">关闭</button>
            <button class="btn-edit" @click="goEdit">✏️ 去编辑</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  chapter: { type: Object, default: null }
})
const emit = defineEmits(['close'])
const router = useRouter()

function goEdit() {
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(0,0,0,0.35); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

.modal-content {
  background: #fff; border-radius: 16px; width: 100%; max-width: 700px;
  max-height: 90vh; overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,0.18);
  animation: dialogIn 0.2s ease;
}

@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px 0;
}

.modal-header h2 {
  font-size: 18px; font-weight: 700; color: #1a1815; margin: 0;
}

.btn-close {
  background: none; border: none; font-size: 18px; color: #9c9690;
  cursor: pointer; padding: 2px 6px; line-height: 1;
}
.btn-close:hover { color: #6b6560; }

.content-body {
  padding: 20px 24px;
}

.text-content {
  font-size: 14px;
  line-height: 1.8;
  color: #3a3530;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  background: #faf8f5;
  border-radius: 10px;
  border: 1px solid #e8e3dc;
}

.empty-content {
  text-align: center;
  padding: 40px 0;
  color: #a8a4a0;
  font-size: 14px;
  font-style: italic;
}

.modal-footer {
  display: flex; justify-content: flex-end; gap: 8px;
  padding: 16px 24px 20px; border-top: 1px solid #f3efe8;
}

.btn-cancel {
  padding: 8px 20px; border: 1.5px solid #e8e3dc; border-radius: 8px;
  background: #fff; color: #6b6560; font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover { background: #f5f2ed; }

.btn-edit {
  padding: 8px 24px; border: none; border-radius: 8px;
  background: #d97706; color: #fff; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
}
.btn-edit:hover { background: #b45309; }

/* 过渡动画 */
.dialog-fade-enter-active { transition: all 0.25s ease-out; }
.dialog-fade-leave-active { transition: all 0.2s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }
</style>