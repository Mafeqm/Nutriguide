/**
 * Configuración central de NutriGuide.
 * Es el ÚNICO lugar donde se define la URL del backend.
 */
window.NUTRIGUIDE_CONFIG = {
  // URL base del backend Spring Boot (sin barra final).
  API_BASE_URL: 'http://localhost:8080',

  // Tiempo máximo de espera (ms)
  REQUEST_TIMEOUT_MS: 15000,
  CHAT_TIMEOUT_MS: 90000,

  // Longitud máxima de una pregunta al asistente
  CHAT_MAX_LENGTH: 500,
};
