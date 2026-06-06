<script setup>
import { computed, onMounted, ref } from 'vue'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const submitError = ref('')
const activity = ref(null)
const registrationResult = ref(null)
const form = ref({
  name: '',
  phone: '',
  attendeeCount: 1,
  unitName: '',
  ageGroup: 'Adult',
  remark: '',
  customValues: {},
})

const activityId = computed(() => {
  const params = new URLSearchParams(window.location.search)
  if (params.get('activityId')) {
    return params.get('activityId')
  }
  if (params.get('id')) {
    return params.get('id')
  }
  const match = window.location.pathname.match(/activities\/([^/]+)/)
  return match ? match[1] : ''
})

const isRegistrationOpen = computed(() => {
  return activity.value?.registrationAvailability === 'OPEN'
})

const availabilityText = computed(() => {
  const value = activity.value?.registrationAvailability
  if (value === 'OPEN') return 'Registration open'
  if (value === 'DEADLINE_PASSED') return 'Deadline passed'
  if (value === 'CAPACITY_FULL') return 'Full'
  return 'Not open'
})

const capacityText = computed(() => {
  if (!activity.value) return ''
  if (activity.value.capacity === null || activity.value.capacity === undefined) {
    return 'Unlimited'
  }
  return `${activity.value.remainingCapacity} / ${activity.value.capacity} remaining`
})

onMounted(async () => {
  if (!activityId.value) {
    error.value = 'Activity id is missing.'
    return
  }
  loading.value = true
  try {
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activityId.value}`)
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || 'Activity not found.')
    }
    activity.value = envelope.data
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})

async function submitRegistration() {
  submitError.value = ''
  submitting.value = true
  try {
    const customValues = activity.value.customFields.map((field) => ({
      fieldKey: field.fieldKey,
      value: form.value.customValues[field.fieldKey] || '',
    }))
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activity.value.id}/registrations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: form.value.name,
        phone: form.value.phone,
        attendeeCount: Number(form.value.attendeeCount),
        unitName: form.value.unitName,
        ageGroup: form.value.ageGroup,
        remark: form.value.remark,
        customValues,
      }),
    })
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || envelope.code || 'Registration failed.')
    }
    registrationResult.value = envelope.data
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="mobile-page">
    <section v-if="loading" class="screen loading-screen">
      <p>Loading activity...</p>
    </section>

    <section v-else-if="error" class="screen empty-screen">
      <p class="eyebrow">Activity</p>
      <h1>Unavailable</h1>
      <p class="summary">{{ error }}</p>
    </section>

    <section v-else-if="registrationResult" class="screen success-screen">
      <p class="eyebrow">Registration Successful</p>
      <h1>{{ registrationResult.activityTitle }}</h1>
      <dl class="facts success-facts">
        <div>
          <dt>Name</dt>
          <dd>{{ registrationResult.name }}</dd>
        </div>
        <div>
          <dt>Phone</dt>
          <dd>{{ registrationResult.phone }}</dd>
        </div>
        <div>
          <dt>Count</dt>
          <dd>{{ registrationResult.attendeeCount }}</dd>
        </div>
      </dl>
      <p class="summary">Please keep your phone available for on-site check-in.</p>
      <button type="button" @click="registrationResult = null">Back to Activity</button>
    </section>

    <section v-else-if="activity" class="activity-detail">
      <div class="cover">
        <img v-if="activity.coverUrl" :src="activity.coverUrl" alt="" />
        <div v-else class="cover-fallback">{{ activity.title.slice(0, 1) }}</div>
      </div>

      <div class="content">
        <div class="title-row">
          <h1>{{ activity.title }}</h1>
          <span class="status" :class="{ open: isRegistrationOpen }">{{ availabilityText }}</span>
        </div>

        <dl class="facts">
          <div>
            <dt>Time</dt>
            <dd>{{ activity.startTime }} - {{ activity.endTime }}</dd>
          </div>
          <div>
            <dt>Location</dt>
            <dd>{{ activity.location }}</dd>
          </div>
          <div>
            <dt>Capacity</dt>
            <dd>{{ capacityText }}</dd>
          </div>
          <div>
            <dt>Deadline</dt>
            <dd>{{ activity.registrationDeadline || 'No deadline' }}</dd>
          </div>
          <div>
            <dt>Contact</dt>
            <dd>{{ activity.ownerName || 'Organizer' }} {{ activity.contactPhone || '' }}</dd>
          </div>
        </dl>

        <p v-if="activity.registrationUnavailableReason" class="notice">
          {{ activity.registrationUnavailableReason }}
        </p>

        <section class="section">
          <h2>Description</h2>
          <p>{{ activity.description || 'No description.' }}</p>
        </section>

        <section v-if="activity.processItems.length" class="section">
          <h2>Process</h2>
          <ol class="timeline">
            <li v-for="item in activity.processItems" :key="item.id">
              <span>{{ item.timeLabel }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </li>
          </ol>
        </section>

        <section class="section">
          <h2>Registration</h2>
          <form class="registration-form" @submit.prevent="submitRegistration">
            <label>
              Name
              <input v-model.trim="form.name" type="text" required :disabled="!isRegistrationOpen || submitting" />
            </label>
            <label>
              Phone
              <input v-model.trim="form.phone" type="tel" required :disabled="!isRegistrationOpen || submitting" />
            </label>
            <label>
              Attendee count
              <input v-model.number="form.attendeeCount" type="number" min="1" required :disabled="!isRegistrationOpen || submitting" />
            </label>
            <label>
              Unit or school
              <input v-model.trim="form.unitName" type="text" :disabled="!isRegistrationOpen || submitting" />
            </label>
            <label>
              Age group
              <select v-model="form.ageGroup" :disabled="!isRegistrationOpen || submitting">
                <option>Adult</option>
                <option>Student</option>
                <option>Child</option>
              </select>
            </label>
            <label>
              Remark
              <textarea v-model.trim="form.remark" rows="3" :disabled="!isRegistrationOpen || submitting"></textarea>
            </label>

            <label v-for="field in activity.customFields" :key="field.id">
              {{ field.label }}<span v-if="field.required" class="required">*</span>
              <select
                v-if="field.fieldType === 'SELECT'"
                v-model="form.customValues[field.fieldKey]"
                :required="field.required"
                :disabled="!isRegistrationOpen || submitting"
              >
                <option v-for="option in field.options" :key="option">{{ option }}</option>
              </select>
              <input
                v-else-if="field.fieldType === 'NUMBER'"
                v-model="form.customValues[field.fieldKey]"
                type="number"
                :required="field.required"
                :disabled="!isRegistrationOpen || submitting"
              />
              <textarea
                v-else-if="field.fieldType === 'MULTI_SELECT'"
                v-model.trim="form.customValues[field.fieldKey]"
                rows="2"
                :required="field.required"
                :disabled="!isRegistrationOpen || submitting"
              ></textarea>
              <input
                v-else
                v-model.trim="form.customValues[field.fieldKey]"
                type="text"
                :required="field.required"
                :disabled="!isRegistrationOpen || submitting"
              />
            </label>

            <p v-if="submitError" class="notice">{{ submitError }}</p>
            <button type="submit" :disabled="!isRegistrationOpen || submitting">
              {{ submitting ? 'Submitting...' : 'Submit Registration' }}
            </button>
          </form>
        </section>
      </div>
    </section>
  </main>
</template>
