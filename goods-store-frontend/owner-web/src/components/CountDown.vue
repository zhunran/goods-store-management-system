<template>
  <span class="countdown" :class="{ urgent: isUrgent, small: props.small }">
    <el-icon><AlarmClock /></el-icon>
    <span v-if="days > 0">{{ days }}天</span>
    <span class="time-chunk" :key="hourStr" :class="{ 'num-pop': anim }">{{
      hourStr
    }}</span>
    <span class="sep">:</span>
    <span class="time-chunk" :key="minStr" :class="{ 'num-pop': anim }">{{
      minStr
    }}</span>
    <span class="sep">:</span>
    <span class="time-chunk" :key="secStr" :class="{ 'num-pop': anim }">{{
      secStr
    }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = defineProps<{
  /** 倒计时总秒数 */
  seconds: number;
  /** 紧凑尺寸（卡片内使用） */
  small?: boolean;
}>();

const emit = defineEmits<{ finish: [] }>();

const remain = ref(Math.max(0, props.seconds));
const anim = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  timer = setInterval(() => {
    if (remain.value <= 0) {
      stop();
      return;
    }
    remain.value -= 1;
    anim.value = true;
    if (remain.value === 0) {
      stop();
      emit("finish");
    }
  }, 1000);
});

onBeforeUnmount(stop);

function stop() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

const days = computed(() => Math.floor(remain.value / 86400));
const hourStr = computed(() => pad(Math.floor((remain.value % 86400) / 3600)));
const minStr = computed(() => pad(Math.floor((remain.value % 3600) / 60)));
const secStr = computed(() => pad(remain.value % 60));
const isUrgent = computed(() => remain.value > 0 && remain.value <= 60);

function pad(n: number) {
  return String(n).padStart(2, "0");
}
</script>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-main);
}

.countdown.urgent {
  color: #ef4444;
}

.time-chunk {
  display: inline-block;
  min-width: 26px;
  padding: 1px 4px;
  border-radius: 6px;
  background: rgba(61, 58, 56, 0.08);
  text-align: center;
}

.urgent .time-chunk {
  background: rgba(239, 68, 68, 0.1);
}

/* 紧凑尺寸 */
.countdown.small {
  font-size: 12px;
  gap: 2px;
}

.countdown.small .time-chunk {
  min-width: 20px;
  padding: 0 3px;
  border-radius: 4px;
}

.countdown.small .sep {
  font-weight: 400;
}

.sep {
  color: var(--text-light);
}
</style>
