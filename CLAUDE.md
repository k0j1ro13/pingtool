# PingMonitor — Instrucciones para Claude Code

## Descripción del proyecto

Aplicación de monitoreo de red multiplataforma (Android, iOS, Desktop) que realiza ping continuo a una IP seleccionada por el usuario, enviando paquetes de distintos tamaños y mostrando el tiempo de respuesta y el estado de cada paquete.

## Funcionalidades principales

- El usuario introduce una IP o hostname destino.
- La app realiza ping continuo (en bucle con corrutinas) hacia esa IP.
- Se envían paquetes de distintos tamaños (32, 64, 128, 256, 512, 1024 bytes) de forma rotativa o configurable.
- Por cada paquete enviado se muestra:
  - Tamaño del paquete (bytes)
  - Tiempo de respuesta (ms)
  - Estado: OK / TIMEOUT / ERROR
  - Número de secuencia
- Resumen de estadísticas en tiempo real: paquetes enviados, recibidos, perdidos (%), RTT mínimo/medio/máximo.
- Posibilidad de pausar y reanudar el ping.

## Stack tecnológico

- **Lenguaje**: Kotlin 2.x
- **Framework UI**: Compose Multiplatform (Android + iOS + Desktop)
- **Arquitectura**: MVVM con `ViewModel` de `lifecycle-viewmodel` KMP
- **Concurrencia**: Kotlin Coroutines + `Flow` para emisión de resultados en tiempo real
- **Ping**:
  - Android/Desktop: `InetAddress.isReachable()` + raw sockets ICMP vía `expect/actual`
  - iOS: subprocess a través de `kotlinx-cinterop` o llamada a `ping` del sistema con `NSTask`
- **Inyección de dependencias**: Koin Multiplatform
- **Persistencia** (historial opcional): SQLDelight
- **Build**: Gradle con `libs.versions.toml` (catálogo de versiones)
- **Contenedor**: Docker para la base de datos (PostgreSQL o equivalente) — la app se conecta al contenedor desde cualquier plataforma

## Estructura de módulos esperada

```
ping-monitor/
├── CLAUDE.md
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── composeApp/                        # Módulo compartido Compose Multiplatform
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/com/pingmonitor/
│       │   ├── App.kt                 # Composable raíz
│       │   ├── ui/
│       │   │   ├── PingScreen.kt      # Pantalla principal
│       │   │   ├── PingRow.kt         # Fila de resultado individual
│       │   │   └── StatsPanel.kt      # Panel de estadísticas
│       │   ├── viewmodel/
│       │   │   └── PingViewModel.kt   # Estado y lógica de UI
│       │   ├── domain/
│       │   │   ├── PingResult.kt      # Modelo de dato
│       │   │   ├── PingUseCase.kt     # Caso de uso: ping continuo
│       │   │   └── StatsCalculator.kt # Cálculo de RTT y pérdida
│       │   └── data/
│       │       └── PingerRepository.kt # Abstracción del ping
│       ├── commonMain/kotlin/.../data/
│       │   └── PingerImpl.expect.kt   # expect para implementación por plataforma
│       ├── androidMain/kotlin/.../data/
│       │   └── PingerImpl.android.kt  # actual Android
│       ├── iosMain/kotlin/.../data/
│       │   └── PingerImpl.ios.kt      # actual iOS
│       └── desktopMain/kotlin/.../data/
│           └── PingerImpl.desktop.kt  # actual Desktop (JVM)
├── iosApp/                            # Punto de entrada iOS (Xcode)
├── docker-compose.yml                 # Orquestación de la base de datos
└── .dockerignore

## Docker

Docker se usa exclusivamente para alojar la base de datos. La app (Android, iOS, Desktop) se conecta al contenedor de base de datos desde cada plataforma.

### Base de datos en Docker

- **Imagen**: `postgres:16-alpine` (ligera y estable)
- **Puerto expuesto**: `5432`
- **Datos persistidos** en un volumen nombrado para no perder el historial entre reinicios
- La app se conecta mediante SQLDelight con el driver JDBC (Desktop/JVM) o el driver nativo de cada plataforma

### docker-compose.yml esperado

```yaml
services:
  db:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: pingmonitor
      POSTGRES_USER: ping
      POSTGRES_PASSWORD: ping_secret
    ports:
      - "5432:5432"
    volumes:
      - pg_data:/var/lib/postgresql/data

volumes:
  pg_data:
```

### Ejemplo de uso

```bash
# Levantar la base de datos
docker compose up -d

# Detener sin borrar datos
docker compose stop

# Borrar todo (incluidos datos)
docker compose down -v
```

## Modelo de datos

```kotlin
data class PingResult(
    val seq: Int,
    val sizeBytes: Int,
    val rttMs: Double?,       // null = timeout o error
    val status: PingStatus,
    val timestamp: Long
)

enum class PingStatus { OK, TIMEOUT, ERROR }

data class PingStats(
    val sent: Int,
    val received: Int,
    val lostPercent: Double,
    val rttMin: Double,
    val rttAvg: Double,
    val rttMax: Double
)
```

## Convenciones de código

- Nombres de clases, funciones y variables en inglés (convención Kotlin estándar).
- Comentarios y mensajes de UI en español.
- Corrutinas lanzadas siempre desde `viewModelScope` o un scope con ciclo de vida controlado.
- `expect/actual` solo para lo estrictamente dependiente de plataforma (el ping en sí).
- Sin lógica de negocio en Composables; todo pasa por el ViewModel.
- Cada archivo tiene una sola clase o función principal pública.

## Comportamiento esperado de la UI

```
┌─────────────────────────────────────────┐
│  PingMonitor                            │
│                                         │
│  IP / Host: [ 8.8.8.8          ] [Ping] │
│  Tamaño de paquete: [Automático  ▼]     │
│  Intervalo: [1.0 s ▼]                   │
│                                         │
│  Seq  Tamaño   RTT       Estado         │
│  ──────────────────────────────────     │
│   1    32 B   12.4 ms   ✓ OK           │
│   2    64 B   13.1 ms   ✓ OK           │
│   3   128 B   —         ✗ TIMEOUT      │
│   4   256 B   14.8 ms   ✓ OK           │
│                                         │
│  Enviados: 4 | Recibidos: 3 | Perdidos: 25%  │
│  RTT mín/med/máx: 12.4 / 13.4 / 14.8 ms    │
│                         [Pausar] [Detener]   │
└─────────────────────────────────────────┘
```

## Reglas para Claude Code

- Responder SIEMPRE en español, sin excepciones.
- No crear ficheros fuera de la estructura definida arriba salvo que se solicite explícitamente.
- No añadir dependencias que no estén en `libs.versions.toml`.
- Antes de modificar cualquier fichero, leerlo completo.
- Usar `expect/actual` únicamente para la implementación del ping por plataforma; el resto es código común.
- Los tests unitarios deben cubrir: `StatsCalculator`, `PingUseCase` (con mock de repositorio) y el modelo `PingResult`.
- No añadir manejo de errores para casos imposibles; confiar en las garantías de Kotlin y Coroutines.
- Preferir `StateFlow` sobre `LiveData` en el ViewModel.
- Docker solo gestiona la base de datos; la app se compila y ejecuta fuera del contenedor en cada plataforma.
- Nunca guardar credenciales de la base de datos en el código; siempre usar variables de entorno o un fichero `.env` (no versionado).
- El volumen de datos (`pg_data`) nunca debe borrarse accidentalmente; usar `docker compose stop` en lugar de `down -v` salvo que se pida expresamente.
