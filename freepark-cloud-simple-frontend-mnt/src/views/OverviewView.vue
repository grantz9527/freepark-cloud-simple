<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <section class="overview">
    <div class="hero">
      <div class="hero-copy">
        <h2 class="brand-mark">{{ t('common.brand') }}</h2>
        <p class="welcome-title">{{ t('classic.welcomeTitle') }}</p>
        <p class="lead">{{ t('classic.welcomeLead') }}</p>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="lot">
          <div class="lot-label">{{ t('classic.lotLabel') }}</div>
          <div class="stalls">
            <span class="stall" />
            <span class="stall occupied teal" />
            <span class="stall" />
            <span class="stall occupied amber" />
            <span class="stall occupied teal" />
            <span class="stall" />
          </div>
        </div>
      </div>
    </div>

    <div class="status">
      <h3>{{ t('classic.statusTitle') }}</h3>
      <p>{{ t('classic.statusBody') }}</p>
    </div>
  </section>
</template>

<style scoped>
.overview {
  max-width: 960px;
}

.hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: clamp(24px, 4vw, 48px);
  align-items: center;
  margin-bottom: 28px;
  animation: fade-up 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-mark {
  margin: 0 0 10px;
  font-family: var(--fp-font-display);
  font-size: clamp(2.2rem, 5vw, 3.2rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
  color: var(--fp-ink);
}

.welcome-title {
  margin: 0 0 12px;
  font-family: var(--fp-font-display);
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--fp-teal);
}

.lead {
  margin: 0;
  max-width: 36ch;
  line-height: 1.65;
  color: var(--fp-ink-soft);
}

.hero-visual {
  display: flex;
  justify-content: flex-end;
}

.lot {
  width: min(260px, 100%);
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(145deg, #16333a, #0f1c24);
  box-shadow: var(--fp-shadow);
}

.lot-label {
  margin-bottom: 12px;
  font-family: var(--fp-font-display);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.18em;
  color: rgba(247, 239, 223, 0.55);
}

.stalls {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.stall {
  aspect-ratio: 3 / 4;
  border-radius: 8px;
  border: 1.5px dashed rgba(247, 239, 223, 0.32);
  background: rgba(255, 255, 255, 0.03);
}

.stall.occupied {
  border-style: solid;
  border-color: transparent;
  animation: occupy 3s ease-in-out infinite;
}

.stall.teal {
  background: linear-gradient(160deg, rgba(13, 122, 111, 0.9), rgba(10, 92, 84, 0.98));
}

.stall.amber {
  background: linear-gradient(160deg, rgba(201, 137, 42, 0.88), rgba(168, 108, 28, 0.98));
  animation-delay: 0.7s;
}

.stall.occupied:nth-child(5) {
  animation-delay: 1.2s;
}

.status {
  padding-top: 22px;
  border-top: 1px solid var(--fp-line);
  animation: fade-up 0.6s 0.1s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.status h3 {
  margin: 0 0 8px;
  font-family: var(--fp-font-display);
  font-size: 1.05rem;
  color: var(--fp-ink);
}

.status p {
  margin: 0;
  max-width: 52ch;
  line-height: 1.65;
  color: var(--fp-muted);
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes occupy {
  0%,
  100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.12);
  }
}

@media (max-width: 720px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    justify-content: flex-start;
  }
}
</style>
