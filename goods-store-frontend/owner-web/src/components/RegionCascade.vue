<template>
  <div class="region-cascade">
    <el-select
      v-model="provinceId"
      placeholder="省"
      clearable
      filterable
      @change="onProvinceChange"
    >
      <el-option
        v-for="p in provinces"
        :key="p.id"
        :label="p.name"
        :value="p.id"
      />
    </el-select>
    <el-select
      v-model="cityId"
      placeholder="市"
      clearable
      filterable
      :disabled="!provinceId"
      @change="onCityChange"
    >
      <el-option
        v-for="c in cities"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </el-select>
    <el-select
      v-model="districtId"
      placeholder="区/县"
      clearable
      filterable
      :disabled="!cityId"
      @change="onDistrictChange"
    >
      <el-option
        v-for="d in districts"
        :key="d.id"
        :label="d.name"
        :value="d.id"
      />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listRegionChildren, type RegionVO } from '@/api/region'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    /** 已保存的完整地址，用于回显时还原省市区选择 */
    initialFull?: string
  }>(),
  { modelValue: '', initialFull: '' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const provinces = ref<RegionVO[]>([])
const cities = ref<RegionVO[]>([])
const districts = ref<RegionVO[]>([])
const provinceId = ref<string | null>(null)
const cityId = ref<string | null>(null)
const districtId = ref<string | null>(null)

function joinRegion(): string {
  const p = provinces.value.find((i) => i.id === provinceId.value)
  const c = cities.value.find((i) => i.id === cityId.value)
  const d = districts.value.find((i) => i.id === districtId.value)
  return [p?.name, c?.name, d?.name].filter(Boolean).join('')
}

function emitRegion() {
  emit('update:modelValue', joinRegion())
}

async function loadProvinces() {
  provinces.value = await listRegionChildren(0)
}

async function loadCities() {
  cities.value = provinceId.value ? await listRegionChildren(provinceId.value) : []
}

async function loadDistricts() {
  districts.value = cityId.value ? await listRegionChildren(cityId.value) : []
}

async function onProvinceChange() {
  cityId.value = null
  districtId.value = null
  cities.value = []
  districts.value = []
  await loadCities()
  emitRegion()
}

async function onCityChange() {
  districtId.value = null
  districts.value = []
  await loadDistricts()
  emitRegion()
}

function onDistrictChange() {
  emitRegion()
}

/** 按名称前缀回填省市区（老数据无分隔时尽力而为，取不到则不报错） */
async function hydrate(full: string) {
  const idx = full.indexOf(' ')
  const region = idx >= 0 ? full.slice(0, idx) : full
  if (!region) return

  const province = provinces.value.find((i) => region.startsWith(i.name))
  if (!province) return
  provinceId.value = province.id
  await loadCities()

  const city = cities.value.find((i) => region.slice(province.name.length).startsWith(i.name))
  if (!city) {
    emitRegion()
    return
  }
  cityId.value = city.id
  await loadDistricts()

  const district = districts.value.find((i) =>
    region.slice(province.name.length + city.name.length).startsWith(i.name),
  )
  districtId.value = district?.id ?? null
  emitRegion()
}

onMounted(async () => {
  await loadProvinces()
  if (props.initialFull) {
    await hydrate(props.initialFull)
  } else if (props.modelValue) {
    // 仅有区域文本、无完整地址时也尝试还原（新地址无需处理）
    await hydrate(props.modelValue)
  }
})
</script>

<style scoped>
.region-cascade {
  display: flex;
  gap: 8px;
  width: 100%;
}

.region-cascade :deep(.el-select) {
  flex: 1;
  min-width: 0;
}
</style>