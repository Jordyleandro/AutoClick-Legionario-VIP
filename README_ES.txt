LEGIONARIO VIP - ANDROID - ITERACION 6.3

Base:
- Continuación del proyecto fuente de la iteración 5.
- Se conservan AutoClick, Flooder, configuración local y editor/reproductor de macros.

Novedades de la iteración 6:
- Nueva pantalla "CAPTURA DE PANTALLA".
- Solicitud del consentimiento oficial de Android mediante MediaProjectionManager.
- Si el usuario acepta, se inicia un servicio de primer plano de tipo mediaProjection.
- Captura puntual de la pantalla completa, guardada como PNG en:
  Android/data/com.autoclicker.android/files/Pictures/LegionarioVIP/
- Vista previa de la captura dentro de la aplicación.
- OCR local con ML Kit (modelo latino integrado): extrae texto de la captura y permite seleccionarlo/copiarlo desde la pantalla.
- Si el usuario cancela el diálogo oficial, no se inicia ninguna captura.
- No se realiza transmisión de pantalla ni grabación continua.

Configuración de referencia:
- Config_Enrike.txt indica INTERVAL_MS=25, BURST=1, BURST_DELAY_MS=30,
  PAUSE_MS=35, MAX_CLICKS=0, FLOODER_INTERVAL_TEXT=2,200.
- Los cinco comandos Flooder tienen activos /action 6, /action 3 y /action 1;
  FLOODER_RANDOM=1 y FLOODER_AUTOCLICK=1.
- SETPOINT_X=1948 y SETPOINT_Y=521 pertenecen a la configuración original
  de escritorio; no se trasladan automáticamente a coordenadas Android.
- Configuracion_Del_AutoClick.txt contiene ajustes de otras funciones de escritorio,
  entre ellos OCR confidence 50, similitud 70, espera CAPTCHA 3-5 s,
  filtros de brillo/oscuridad y opciones de Uppercut. La captura v6.1 incorpora OCR local básico. No implementa todavía CAPTCHA, OpenCV, selector de área, detección visual por plantillas ni lógica Uppercut.

Notas técnicas:
- La autorización de MediaProjection la concede el usuario desde el diálogo oficial
  de Android. Android puede volver a solicitarla en cada nueva sesión.
- La captura se almacena en el directorio privado externo de la aplicación.
- El proyecto fuente no ha sido compilado ni probado en un dispositivo en este entorno. La dependencia de ML Kit requiere acceso a Google Maven/Maven Central durante la sincronización de Gradle.
- Requiere Android Studio, Gradle y Android SDK 35.
- Min SDK 26; target SDK 28 (se conserva el valor del proyecto v5).

Ejemplo de macro:
500,500,300
650,500,500
500,650,1000

Alcance de macros:
- Reproductor básico de toques secuenciales en coordenadas Android.
- No detecta automáticamente personajes ni identifica izquierda/derecha.
- No equivale aún al módulo Uppercut original.

OCR (iteración 6.1):
- Dependencia com.google.mlkit:text-recognition:16.0.1.
- Lee texto latino de la imagen capturada; no interpreta por sí solo botones, estados del juego ni CAPTCHA.
- El texto reconocido se muestra en pantalla y puede seleccionarse manualmente.

ITERACION 6.2 - RECONOCIMIENTO VISUAL POR PLANTILLA
- Se añade ImageRecognitionActivity accesible desde el menú principal.
- El usuario selecciona mediante el selector oficial de documentos una captura y una plantilla de imagen.
- La búsqueda compara muestras de color en una cuadrícula 20x20 y prueba tres escalas cercanas (90%, 100%, 110%).
- Devuelve un porcentaje de similitud aproximado y coordenadas X/Y de la coincidencia candidata.
- Todo el procesamiento se realiza localmente. No se ejecutan toques ni acciones automáticas a partir del resultado.
- Umbral inicial de aceptación: 82%. El algoritmo es experimental y puede fallar con cambios de escala, brillo, compresión, fondos repetidos o animaciones. Verificar manualmente la coincidencia.
- Esta función no es todavía detección semántica de objetos ni reemplaza OpenCV; es una búsqueda básica por similitud de píxeles.


ITERACION 6.3 - REGION DE BUSQUEDA Y UMBRAL CONFIGURABLE
- La pantalla de reconocimiento visual permite definir una región rectangular de búsqueda mediante X, Y, ancho y alto, en píxeles de la captura.
- Al cargar una captura, la región se inicializa con las dimensiones completas de esa imagen.
- Se incorpora un umbral mínimo configurable de similitud entre 50% y 99% (valor inicial 82%).
- La búsqueda usa un recorrido grueso y un refinamiento local de coordenadas para reducir el número de comparaciones y afinar la posición candidata.
- El resultado informa el umbral utilizado, la región analizada y la posición estimada.
- La detección sigue siendo comparación de color por muestras; no es reconocimiento semántico ni garantiza identificar un botón. El usuario debe verificar visualmente cualquier coincidencia.
- El resultado no activa toques automáticamente. La integración con AutoClick queda pendiente y deberá requerir confirmación explícita del usuario.
- Versión Android declarada: versionCode 4, versionName 1.3.

VALIDACION
- Se revisó la estructura y se verificó la integridad del ZIP de entrega.
- No se pudo compilar ni ejecutar en dispositivo en este entorno porque no hay instalación disponible de Gradle/Android SDK.


ITERACION 6.5 - INTEGRACION CONTROLADA DE RECONOCIMIENTO Y AUTOCLICK
- Tras encontrar una coincidencia y guardar explícitamente su centro como punto AutoClick, se habilita un botón independiente para ejecutar un único toque de prueba.
- Antes del toque se muestra un diálogo de confirmación con las coordenadas. Cancelar no ejecuta ninguna acción.
- Se comprueba que el punto esté dentro de las dimensiones actuales de pantalla; si la captura y la pantalla tienen resoluciones u orientaciones incompatibles, se bloquea el toque.
- El toque usa el servicio de Accesibilidad existente y no activa el modo repetitivo de AutoClick.
- Si el servicio no está activo, se informa al usuario y no se ejecuta la acción.
- Se conserva la búsqueda local por plantilla, región configurable y umbral de similitud.
- versionCode 5, versionName 6.5.

VALIDACION 6.5
- Cambios de fuente revisados estáticamente.
- Pendiente compilar y probar en Android Studio con SDK 35 y ML Kit; este entorno no dispone de Gradle/Android SDK instalado.
