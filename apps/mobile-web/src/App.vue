<script setup>
import { computed, onMounted, ref } from 'vue'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const submitError = ref('')
const activity = ref(null)
const registrationResult = ref(null)
const checkInResult = ref(null)
const volunteerResult = ref(null)
const volunteerPositions = ref([])
const selectedPositionId = ref('')
const volunteerAttendanceStatus = ref(null)
const form = ref({
  name: '',
  phone: '',
  attendeeCount: 1,
  unitName: '',
  ageGroup: 'Adult',
  remark: '',
  customValues: {},
})
const checkInForm = ref({
  phone: '',
})
const volunteerForm = ref({
  name: '',
  phone: '',
  unitName: '',
  ageGroup: 'Adult',
  availableTimeNote: '',
  experienceNote: '',
  remark: '',
})
const volunteerAttendanceForm = ref({
  phone: '',
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

const isCheckInMode = computed(() => {
  const params = new URLSearchParams(window.location.search)
  return params.get('mode') === 'check-in' || window.location.pathname.includes('/check-in')
})

const isVolunteerMode = computed(() => {
  const params = new URLSearchParams(window.location.search)
  return (params.get('mode') === 'volunteers' || window.location.pathname.includes('/volunteers')) && !isVolunteerAttendanceMode.value
})

const isVolunteerAttendanceMode = computed(() => {
  const params = new URLSearchParams(window.location.search)
  return params.get('mode') === 'volunteer-attendance' || window.location.pathname.includes('/volunteers/attendance')
})

const selectedPosition = computed(() => {
  return volunteerPositions.value.find((position) => position.id === selectedPositionId.value)
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
    if (isVolunteerMode.value) {
      await loadVolunteerPositions()
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
})

async function loadVolunteerPositions() {
  const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activityId.value}/volunteer-positions`)
  const envelope = await response.json()
  if (!response.ok || !envelope.success) {
    throw new Error(envelope.message || envelope.code || 'Volunteer positions unavailable.')
  }
  volunteerPositions.value = envelope.data
  if (!selectedPositionId.value && volunteerPositions.value.length > 0) {
    selectedPositionId.value = volunteerPositions.value[0].id
  }
}

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

async function submitCheckIn() {
  submitError.value = ''
  submitting.value = true
  try {
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activity.value.id}/check-ins`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        phone: checkInForm.value.phone,
      }),
    })
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || envelope.code || 'Check-in failed.')
    }
    checkInResult.value = envelope.data
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitting.value = false
  }
}

async function submitVolunteerApplication() {
  submitError.value = ''
  submitting.value = true
  try {
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activity.value.id}/volunteer-applications`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        positionId: selectedPositionId.value,
        name: volunteerForm.value.name,
        phone: volunteerForm.value.phone,
        unitName: volunteerForm.value.unitName,
        ageGroup: volunteerForm.value.ageGroup,
        availableTimeNote: volunteerForm.value.availableTimeNote,
        experienceNote: volunteerForm.value.experienceNote,
        remark: volunteerForm.value.remark,
      }),
    })
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || envelope.code || 'Volunteer application failed.')
    }
    volunteerResult.value = envelope.data
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitting.value = false
  }
}

async function lookupVolunteerAttendance() {
  submitError.value = ''
  volunteerAttendanceStatus.value = null
  submitting.value = true
  try {
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activity.value.id}/volunteer-applications/status`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        phone: volunteerAttendanceForm.value.phone,
      }),
    })
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || envelope.code || 'Volunteer status lookup failed.')
    }
    volunteerAttendanceStatus.value = envelope.data
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitting.value = false
  }
}

async function volunteerCheckIn() {
  await submitVolunteerAttendanceAction('check-in')
}

async function volunteerCheckOut() {
  await submitVolunteerAttendanceAction('check-out')
}

async function submitVolunteerAttendanceAction(action) {
  submitError.value = ''
  submitting.value = true
  try {
    const applicationId = volunteerAttendanceStatus.value.application.id
    const response = await fetch(`${apiBaseUrl}/api/mobile/activities/${activity.value.id}/volunteer-applications/${applicationId}/${action}`, {
      method: 'POST',
    })
    const envelope = await response.json()
    if (!response.ok || !envelope.success) {
      throw new Error(envelope.message || envelope.code || 'Volunteer attendance update failed.')
    }
    volunteerAttendanceStatus.value = {
      ...volunteerAttendanceStatus.value,
      attendance: envelope.data,
    }
  } catch (err) {
    submitError.value = err.message
  } finally {
    submitting.value = false
  }
}

function navigateTo(url) {
  window.location.href = url
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

    <section v-else-if="checkInResult" class="screen success-screen">
      <p class="eyebrow">Check-in Successful</p>
      <h1>{{ checkInResult.activityTitle }}</h1>
      <dl class="facts success-facts">
        <div>
          <dt>Name</dt>
          <dd>{{ checkInResult.name }}</dd>
        </div>
        <div>
          <dt>Time</dt>
          <dd>{{ checkInResult.checkInTime }}</dd>
        </div>
      </dl>
      <button type="button" @click="checkInResult = null">Back</button>
    </section>

    <section v-else-if="volunteerResult" class="screen success-screen">
      <p class="eyebrow">Volunteer Application Submitted</p>
      <h1>{{ volunteerResult.activityTitle }}</h1>
      <dl class="facts success-facts">
        <div>
          <dt>Position</dt>
          <dd>{{ volunteerResult.positionName }}</dd>
        </div>
        <div>
          <dt>Name</dt>
          <dd>{{ volunteerResult.name }}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{{ volunteerResult.status }}</dd>
        </div>
      </dl>
      <p class="summary">Please wait for the organizer to review your application.</p>
      <button type="button" @click="volunteerResult = null">Back to Positions</button>
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

        <section v-if="isVolunteerAttendanceMode" class="section">
          <h2>Volunteer Service</h2>
          <form class="registration-form" @submit.prevent="lookupVolunteerAttendance">
            <label>
              Phone
              <input v-model.trim="volunteerAttendanceForm.phone" type="tel" required :disabled="submitting" />
            </label>
            <button type="submit" :disabled="submitting">
              {{ submitting ? 'Looking up...' : 'Lookup Status' }}
            </button>
          </form>

          <div v-if="volunteerAttendanceStatus" class="attendance-panel">
            <dl class="facts success-facts">
              <div>
                <dt>Name</dt>
                <dd>{{ volunteerAttendanceStatus.application.name }}</dd>
              </div>
              <div>
                <dt>Position</dt>
                <dd>{{ volunteerAttendanceStatus.application.positionName }}</dd>
              </div>
              <div>
                <dt>Apply</dt>
                <dd>{{ volunteerAttendanceStatus.application.status }}</dd>
              </div>
              <div>
                <dt>Attend</dt>
                <dd>{{ volunteerAttendanceStatus.attendance?.status || 'NOT_CHECKED_IN' }}</dd>
              </div>
              <div>
                <dt>Check-in</dt>
                <dd>{{ volunteerAttendanceStatus.attendance?.checkInTime || '-' }}</dd>
              </div>
              <div>
                <dt>Check-out</dt>
                <dd>{{ volunteerAttendanceStatus.attendance?.checkOutTime || '-' }}</dd>
              </div>
              <div>
                <dt>Minutes</dt>
                <dd>{{ volunteerAttendanceStatus.attendance?.effectiveServiceMinutes ?? 0 }}</dd>
              </div>
            </dl>
            <div class="action-row">
              <button
                type="button"
                :disabled="submitting || volunteerAttendanceStatus.application.status !== 'APPROVED' || volunteerAttendanceStatus.attendance?.status === 'CHECKED_IN' || volunteerAttendanceStatus.attendance?.status === 'CHECKED_OUT'"
                @click="volunteerCheckIn"
              >
                Check In
              </button>
              <button
                type="button"
                :disabled="submitting || volunteerAttendanceStatus.application.status !== 'APPROVED' || volunteerAttendanceStatus.attendance?.status !== 'CHECKED_IN'"
                @click="volunteerCheckOut"
              >
                Check Out
              </button>
            </div>
          </div>

          <p v-if="submitError" class="notice">{{ submitError }}</p>
          <button type="button" class="secondary-button" @click="navigateTo(activity.volunteerLink)">
            Volunteer Apply
          </button>
        </section>

        <section v-else-if="isVolunteerMode" class="section">
          <h2>Volunteer Positions</h2>
          <div v-if="volunteerPositions.length" class="position-list">
            <label
              v-for="position in volunteerPositions"
              :key="position.id"
              class="position-card"
              :class="{ selected: selectedPositionId === position.id, full: position.full }"
            >
              <input v-model="selectedPositionId" type="radio" name="position" :value="position.id" :disabled="position.full || submitting" />
              <span>
                <strong>{{ position.name }}</strong>
                <small>{{ position.description || 'No description.' }}</small>
                <small>{{ position.serviceStartTime }} - {{ position.serviceEndTime }}</small>
                <small>{{ position.approvedCount }} approved / {{ position.capacity }} capacity</small>
              </span>
            </label>
          </div>
          <p v-else class="notice">No volunteer positions are available.</p>

          <form class="registration-form" @submit.prevent="submitVolunteerApplication">
            <label>
              Name
              <input v-model.trim="volunteerForm.name" type="text" required :disabled="!selectedPosition || selectedPosition.full || submitting" />
            </label>
            <label>
              Phone
              <input v-model.trim="volunteerForm.phone" type="tel" required :disabled="!selectedPosition || selectedPosition.full || submitting" />
            </label>
            <label>
              Unit or school
              <input v-model.trim="volunteerForm.unitName" type="text" :disabled="!selectedPosition || selectedPosition.full || submitting" />
            </label>
            <label>
              Age group
              <select v-model="volunteerForm.ageGroup" :disabled="!selectedPosition || selectedPosition.full || submitting">
                <option>Adult</option>
                <option>Student</option>
                <option>Child</option>
              </select>
            </label>
            <label>
              Available time
              <textarea v-model.trim="volunteerForm.availableTimeNote" rows="2" :disabled="!selectedPosition || selectedPosition.full || submitting"></textarea>
            </label>
            <label>
              Experience
              <textarea v-model.trim="volunteerForm.experienceNote" rows="2" :disabled="!selectedPosition || selectedPosition.full || submitting"></textarea>
            </label>
            <label>
              Remark
              <textarea v-model.trim="volunteerForm.remark" rows="3" :disabled="!selectedPosition || selectedPosition.full || submitting"></textarea>
            </label>

            <p v-if="submitError" class="notice">{{ submitError }}</p>
            <button type="submit" :disabled="!selectedPosition || selectedPosition.full || submitting">
              {{ submitting ? 'Submitting...' : 'Submit Volunteer Application' }}
            </button>
          </form>
          <button type="button" class="secondary-button" @click="navigateTo(activity.registrationLink)">
            Registration
          </button>
          <button type="button" class="secondary-button" @click="navigateTo(`${activity.volunteerLink}/attendance`)">
            Volunteer Service
          </button>
        </section>

        <section v-else-if="isCheckInMode" class="section">
          <h2>Activity Check-in</h2>
          <form class="registration-form" @submit.prevent="submitCheckIn">
            <label>
              Phone
              <input v-model.trim="checkInForm.phone" type="tel" required :disabled="submitting" />
            </label>

            <p v-if="submitError" class="notice">{{ submitError }}</p>
            <button type="submit" :disabled="submitting">
              {{ submitting ? 'Checking in...' : 'Check In' }}
            </button>
          </form>
          <button type="button" class="secondary-button" @click="navigateTo(activity.registrationLink)">
            Registration
          </button>
        </section>

        <section v-else class="section">
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
          <button type="button" class="secondary-button" @click="navigateTo(activity.checkInLink)">
            Check-in
          </button>
          <button type="button" class="secondary-button" @click="navigateTo(activity.volunteerLink)">
            Volunteer
          </button>
        </section>
      </div>
    </section>
  </main>
</template>
