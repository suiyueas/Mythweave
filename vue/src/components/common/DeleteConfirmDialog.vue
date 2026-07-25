<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="visible" class="dialog-overlay" @click.self="handleCancel">
        <div class="dialog-content">
          <div class="dialog-icon">⚠️</div>
          <h3 class="dialog-title">确认删除</h3>
          <p class="dialog-message">确定要删除「<strong>{{ work?.title || '未命名' }}</strong>」吗？</p>
          <p class="dialog-hint">此操作不可恢复，作品的所有章节和设定将永久删除。</p>
          <div class="dialog-actions">
            <button class="btn-cancel" @click="handleCancel" :disabled="loading">取消</button>
            <button class="btn-delete" @click="handleConfirm" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span v-else>🗑️</span>
              {{ loading ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  work: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = computed(() => props.modelValue)

function handleConfirm() {
  if (!props.loading) emit('confirm')
}

function handleCancel() {
  if (!props.loading) emit('cancel')
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0, 0, 0, 0.45); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  padding: 20px;
}

.dialog-content {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px 36px 28px;
  max-width: 420px;
  width: 100%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.dialog-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.dialog-message {
  font-size: 14px;
  color: #6b6560;
  margin: 0 0 6px;
  line-height: 1.5;
}

.dialog-message strong {
  color: #1a1a2e;
}

.dialog-hint {
  font-size: 12px;
  color: #a8a4a0;
  margin: 0 0 24px;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn-cancel {
  padding: 10px 28px;
  background: #f5f2ed;
  border: 1px solid #e8e3dc;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #6b6560;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover:not(:disabled) {
  background: #e8e3dc;
}

.btn-delete {
  padding: 10px 28px;
  background: #be123c;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.btn-delete:hover:not(:disabled) {
  background: #9f1239;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(190, 18, 60, 0.3);
}

.btn-delete:disabled, .btn-cancel:disabled {
  opacity: 0.6;
  cursor: wait;
}

.spinner {
  display: inline-block;
  width: 16px; height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.dialog-fade-enter-active { transition: all 0.25s ease-out; }
.dialog-fade-leave-active { transition: all 0.2s ease-in; }
.dialog-fade-enter-from { opacity: 0; }
.dialog-fade-enter-from .dialog-content { transform: scale(0.92); opacity: 0; }
.dialog-fade-leave-to { opacity: 0; }
.dialog-fade-leave-to .dialog-content { transform: scale(0.92); opacity: 0; }
</style>
