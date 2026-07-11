# Xenon Lite

Mod de cliente para Minecraft Forge 1.8.9 con módulos de combate configurables.

## Requisitos

- JDK 8 (ForgeGradle 2.1 no funciona correctamente con JDK modernos).
- El Gradle Wrapper incluido en el repositorio.

## Preparación

```bash
./gradlew setupDecompWorkspace
./gradlew idea
```

Para iniciar el cliente de desarrollo:

```bash
./gradlew runClient
```

Para producir el JAR:

```bash
./gradlew build
```

El JAR reobfuscado queda en `build/libs/xenonlite-1.0.0.jar`.

## Módulos de combate

Los módulos persistentes se registran en el bus de eventos y quedan desactivados
por defecto. `ThrowPot` es una acción puntual y se ejecuta mediante `toggle()` o
con su keybind, que inicialmente es la tecla `F`.

```java
XenonLite.instance.getLeftClicker().setEnabled(true);
XenonLite.instance.getRightClicker().setEnabled(true);
XenonLite.instance.getRefill().setEnabled(true);
XenonLite.instance.getThrowPot().toggle();
XenonLite.instance.getAimAssist().setEnabled(true);
XenonLite.instance.getReach().setEnabled(true);
XenonLite.instance.getVelocity().setEnabled(true);
```

Si no se usa una instancia estática de `XenonLite`, conserva la instancia que Forge
inyecta o conecta el método `setEnabled` directamente desde tu gestor de módulos.

La interfaz se abre con `Right Shift`. Su tamaño inicial se adapta a la resolución
y al `GUI Scale` de Minecraft, y nunca usa un mínimo fijo mayor que el área útil.
Permite activar módulos, modificar sus ajustes y redimensionar el panel desde la
esquina inferior derecha. La rueda del ratón desplaza de forma independiente la
lista de módulos o la lista de ajustes según la columna bajo el cursor; ambas áreas
usan recorte para impedir que el contenido salga del panel. El keybind de
`ThrowPot` se puede capturar desde la propia interfaz; `Delete` o `Backspace` lo
elimina y `Escape` cancela la captura.

## Cálculos de Aim Assist

### Ruido Perlin continuo

`AimAssist` usa dos instancias independientes de `PerlinNoise`, una para yaw y otra
para pitch. Cada instancia crea una permutación pseudoaleatoria de 256 entradas y
evalúa ruido Perlin unidimensional mediante gradientes, interpolación lineal y la
curva de suavizado:

```text
fade(t) = t³ × (t × (t × 6 - 15) + 10)
```

La coordenada temporal se calcula como:

```text
time = (ticksExisted + partialTick) × noiseSpeed
```

El canal de pitch se evalúa con un desplazamiento de `137.31` para evitar que sus
valores coincidan con los de yaw. No se suma directamente la muestra completa en
cada frame, porque eso produciría deriva acumulativa. Se aplica la diferencia
entre la muestra actual y la anterior:

```text
yawDelta   = (yawNoise[n]   - yawNoise[n-1])   × strength
pitchDelta = (pitchNoise[n] - pitchNoise[n-1]) × strength × 0.65
```

Esto hace que el valor del frame `N` esté fuertemente correlacionado con el de
`N-1`, produciendo curvas continuas. La intensidad vertical se reduce al 65 % y el
pitch final se limita a `[-90°, 90°]`. También se actualizan `rotationYaw/Pitch` y
sus valores `prevRotationYaw/Pitch` para conservar la interpolación visual.

El estado de ruido se reinicializa al perder o cambiar de objetivo, dejar de hacer
clic, cambiar a un objeto no permitido o desactivar el módulo. Así se evita aplicar
una diferencia grande después de un periodo sin muestras.

Ajustes disponibles:

- `Noise`: intensidad entre `0.0` y `2.0`; valor inicial `0.35`.
- `Noise Speed`: avance temporal entre `0.01` y `0.30`; valor inicial `0.08`.

## Cálculos de Throw Pot

### Distribución log-normal truncada

La espera fija fue sustituida por una muestra log-normal limitada al intervalo de
`200–300 ms`. El ajuste `Speed` representa el centro solicitado y también está
limitado a ese rango; su valor inicial es `250 ms`.

Para cada lanzamiento se usan:

```text
sigma = 0.12
mu = ln(center) - sigma² / 2
delay = round(exp(mu + sigma × gaussian()))
```

Se realizan hasta 16 intentos para obtener una muestra dentro de `[200, 300]`. Si
ninguno resulta válido, se utiliza el centro configurado, previamente limitado al
mismo intervalo. Frente a una distribución uniforme, la log-normal concentra más
resultados alrededor del centro y conserva una cola asimétrica.

### Cadena de estados con desvío

Los estados de slot se definen así:

```text
A = slot original
B = slot que contiene la poción
C = slot de desvío, siempre diferente de A y B
```

Con `Detour` desactivado, la secuencia es:

```text
A → B → A
```

Con `Detour` activado, cada lanzamiento puede seguir esa ruta o:

```text
A → C → B → A
```

Es un modelo inspirado en una cadena de Markov, pero no implementa una matriz de
transición clásica: la elección de ruta está correlacionada mediante una muestra
Perlin evaluada con el contador de secuencia. El umbral actual para tomar el desvío
es `0.52` después de normalizar la muestra aproximadamente a `[0, 1]`.

Perlin también determina el punto inicial para buscar `C` y la fracción del tiempo
total asignada a `A → C → B`. Esa fracción está entre el 12 % y el 25 %:

```text
detourRatio = 0.12 + normalizedNoise × 0.13
detourDelay = totalDelay × detourRatio
```

La suma de las esperas de cualquier ruta conserva la muestra log-normal total:

```text
detourDelay + (totalDelay - detourDelay) = totalDelay
```

Por tanto, el presupuesto de espera sigue dentro de `200–300 ms`. La latencia real
de extremo a extremo puede ser algo mayor si el cliente tarda en procesar una tarea
programada.

### Hilo auxiliar y game loop

`ThrowPot` crea un hilo daemon independiente. En ese hilo se calculan la muestra,
la ruta y las esperas mediante `Thread.sleep`, de modo que nunca se bloquea el hilo
principal de Minecraft.

Los cambios de slot y la pulsación de uso no se ejecutan directamente desde el
hilo auxiliar. Se envían mediante `Minecraft.addScheduledTask()` y el hilo auxiliar
espera su finalización. Esto garantiza orden y evita modificar inventario o teclas
concurrentemente:

```text
hilo auxiliar: calcula → programa estado → espera → programa siguiente estado
hilo principal: aplica slot/tecla y sincroniza PlayerControllerMP
```

Un servidor nominal de 20 TPS procesa ticks cada 50 ms, pero el cliente no conoce
la frontera exacta del tick remoto. Latencia, renderizado, cola de red y carga del
servidor pueden agrupar o desplazar la observación de paquetes. La implementación
no intenta sincronizar ni desincronizar paquetes con el tick del servidor: mantiene
una temporización local no bloqueante y deja que Minecraft gestione el envío.

La restauración de `A` se ejecuta también desde `finally` si hay una interrupción o
falla una tarea. La bandera `throwing` impide iniciar dos secuencias simultáneas.

## Módulo Velocity

`Velocity` modifica el movimiento recibido en el primer tick de daño, cuando
`hurtResistantTime == maxHurtResistantTime`. Solo procesa el evento correspondiente
al jugador local y como máximo una vez por `ticksExisted`.

- `Vert`: multiplicador de `motionY`, entre `0.0` y `2.0`.
- `Hor`: multiplicador de `motionX` y `motionZ`, entre `0.0` y `2.0`.
- `Chance`: probabilidad configurada entre `2` y `100`.

La comprobación conserva la comparación inclusiva del código original:
`random.nextInt(100) <= Chance`.

## Módulo Reach

`Reach` recalcula el raycast del cursor cuando Forge emite un evento de ratón.
El rango se escoge aleatoriamente entre `Min` y `Max`, condicionado por `Chance`.

- `Min` y `Max`: alcance entre `3.0` y `6.0` bloques.
- `Chance`: comparación inclusiva contra un entero de `0` a `100`.
- `Walls`: permite continuar cuando el raycast actual encuentra un bloque sólido.
- `Sprint`: exige que el jugador esté corriendo.

El raycast conserva la prioridad del bloque más cercano y solo sustituye
`objectMouseOver` cuando una entidad válida está más cerca que dicho impacto.
